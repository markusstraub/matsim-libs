package org.matsim.vis.otfvis2;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.SnapshotGenerator;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.vis.otfvis.SimulationViewForQueries;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.OTFServerQuadI;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.interfaces.OTFLiveServerRemote;
import org.matsim.vis.otfvis.interfaces.OTFQueryRemote;
import org.matsim.vis.otfvis.opengl.queries.AbstractQuery;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.SnapshotWriter;

public final class OTFVisLiveServer implements OTFLiveServerRemote {

	private QueryServer queryServer;

	private final OTFAgentsListHandler.Writer agentWriter = new OTFAgentsListHandler.Writer();

	private MyQuadTree quadTree;

	private volatile boolean synchedPlayback = true;

	private volatile boolean finished = false;

	private TimeStep nextTimeStep;

	private ArrayBlockingQueue<TimeStep> timeStepBuffer = new ArrayBlockingQueue<TimeStep>(1);

	private final ByteBuffer byteBuffer = ByteBuffer.allocate(80000000);

	private Scenario scenario;

	private SnapshotReceiver snapshotReceiver;

	private SnapshotGenerator snapshotGenerator;

	private Map<Id, Plan> plans;

	private final class CurrentTimeStepView implements SimulationViewForQueries {

		@Override
		public Collection<AgentSnapshotInfo> getSnapshot() {
			return nextTimeStep.agentPositions;
		}

		@Override
		public Map<Id, Plan> getPlans() {
			return plans;
		}

		@Override
		public Network getNetwork() {
			return scenario.getNetwork();
		}

	}

	private static class TimeStep implements Serializable {

		private static final long serialVersionUID = 1L;

		public Collection<AgentSnapshotInfo> agentPositions = new ArrayList<AgentSnapshotInfo>();

		public int time;

	}

	private class MyQuadTree extends OTFServerQuad2 {

		private static final long serialVersionUID = 1L;

		public MyQuadTree() {
			super(scenario.getNetwork());
		}

		@Override
		public void initQuadTree(OTFConnectionManager connect) {
			initQuadTree();
		}

		private void initQuadTree() {
			for (Link link : scenario.getNetwork().getLinks().values()) {
				double middleEast = (link.getToNode().getCoord().getX() + link.getFromNode().getCoord().getX()) * 0.5 - this.minEasting;
				double middleNorth = (link.getToNode().getCoord().getY() + link.getFromNode().getCoord().getY()) * 0.5 - this.minNorthing;
				LinkHandler.Writer linkWriter = new LinkHandler.Writer();
				linkWriter.setSrc(link);
				this.put(middleEast, middleNorth, linkWriter);
			}
			this.addAdditionalElement(agentWriter);
		}

	}

	private class SnapshotReceiver implements SnapshotWriter {

		private TimeStep timeStep;

		@Override
		public void addAgent(AgentSnapshotInfo position) {
			if (position.getAgentState() == AgentSnapshotInfo.AgentState.PERSON_AT_ACTIVITY) return;
			timeStep.agentPositions.add(position);
		}

		@Override
		public void beginSnapshot(double time) {
			timeStep = new TimeStep();
			timeStep.time = (int) time;
		}

		@Override
		public void endSnapshot() {
			putTimeStep(timeStep);
		}

		private void putTimeStep(TimeStep timeStep2) {
			if (!synchedPlayback) {
				timeStepBuffer.clear();
				nextTimeStep = timeStep;
			}
			try {
				timeStepBuffer.put(timeStep);
			} catch (InterruptedException e) {

			}
		}

		@Override
		public void finish() {
			finished = true;
		}

	}

	public OTFVisLiveServer(Scenario scenario, EventsManager eventsManager) {
		this.scenario = scenario;
		this.snapshotReceiver = new SnapshotReceiver();
		this.quadTree = new MyQuadTree();
		this.quadTree.initQuadTree();
		SimulationViewForQueries queueModel = new CurrentTimeStepView();
		this.queryServer = new QueryServer(scenario, eventsManager, queueModel);
		this.nextTimeStep = new TimeStep();
		this.plans = new HashMap<Id, Plan>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			this.plans.put(person.getId(), plan);
		}
	}

	@Override
	public OTFQueryRemote answerQuery(AbstractQuery query)
	throws RemoteException {
		return queryServer.answerQuery(query);
	}

	@Override
	public void pause() throws RemoteException {
		synchedPlayback = true;
	}

	@Override
	public void play() throws RemoteException {
		synchedPlayback = false;
		timeStepBuffer.clear();
	}

	@Override
	public void removeQueries() throws RemoteException {
		queryServer.removeQueries();
	}

	@Override
	public int getLocalTime() throws RemoteException {
		if (nextTimeStep == null) {
			return 0;
		} else {
			return nextTimeStep.time;
		}
	}

	@Override
	public OTFVisConfigGroup getOTFVisConfig() throws RemoteException {
		return new OTFVisConfigGroup();
	}

	@Override
	public OTFServerQuadI getQuad(String id, OTFConnectionManager connect) throws RemoteException {
		return quadTree;
	}

	@Override
	public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
		byte[] result;
		byteBuffer.position(0);
		quadTree.writeConstData(byteBuffer);
		int pos = byteBuffer.position();
		result = new byte[pos];
		byteBuffer.position(0);
		byteBuffer.get(result);
		return result;
	}

	@Override
	public byte[] getQuadDynStateBuffer(String id, Rect bounds)
	throws RemoteException {
		byte[] result;
		byteBuffer.position(0);
		agentWriter.positions.clear();
		if (nextTimeStep != null) {
			agentWriter.positions.addAll(nextTimeStep.agentPositions);
		}
		quadTree.writeDynData(bounds, byteBuffer);
		int pos = byteBuffer.position();
		result = new byte[pos];
		byteBuffer.position(0);
		byteBuffer.get(result);
		return result;
	}

	@Override
	public Collection<Double> getTimeSteps() throws RemoteException {
		return null;
	}

	@Override
	public boolean isLive() throws RemoteException {
		return true;
	}

	@Override
	public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
		if (!finished) {
			if (snapshotGenerator != null) {
				snapshotGenerator.skipUntil(time);
			}
			while(nextTimeStep == null || nextTimeStep.time < time) {
				try {
					nextTimeStep = timeStepBuffer.take();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			return true;
		} else {
			timeStepBuffer.clear();
			return false;
		}
	}

	@Override
	public void toggleShowParking() throws RemoteException {

	}

	public SnapshotWriter getSnapshotReceiver() {
		return snapshotReceiver;
	}

	public void setSnapshotGenerator(SnapshotGenerator snapshotGenerator) {
		this.snapshotGenerator = snapshotGenerator;
	}

	public void addAdditionalPlans(Map<Id, Plan> additionalPlans) {
		this.plans.putAll(additionalPlans);

	}

}
