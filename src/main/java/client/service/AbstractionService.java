package client.service;

import client.handlers.app.AppBase;
import client.handlers.registry.NNAtomicRegistry;
import client.handlers.uniform_consensus.UniformConsensus;
import client.model.ProcessingResult;
import client.model.SystemContext;
import client.protobuf.EpochConsensusMessageUtils;
import client.protobuf.MessageUtils;
import client.protobuf.P;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class AbstractionService {
    private static final Logger log = LogManager.getLogger(AbstractionService.class);
    private static final String ABSTRACTION_ID = "app";

    private final Map<String, Function<P.Message, ProcessingResult>> abstractions;
    private final SystemContext context;

    public AbstractionService(SystemContext systemContext) {
        this.context = systemContext;
        this.abstractions = new HashMap<>(60);
    }

    public ProcessingResult sendMessageToAbstraction(String toAbstraction, P.Message message) {
        if (toAbstraction.startsWith("app.nnar") && !abstractions.containsKey(toAbstraction)) {
            String register = MessageUtils.getRegisterFromAbstraction(toAbstraction);
            registerNewNNAtomicRegistry(register);
        } else if (toAbstraction.startsWith("app.uc") && !abstractions.containsKey(toAbstraction)) {
            String topic = MessageUtils.getRegisterFromAbstraction(toAbstraction);
            registerNewUniformConsensus(topic);
        }
        return abstractions.get(toAbstraction).apply(message);
    }

    public void registerAbstractions(AppBase app) {
        abstractions.put("app", (P.Message message) -> app.apply(message));
        abstractions.put("app.pl", (P.Message message) -> app.getPerfectLink().apply(message));
        abstractions.put("app.beb", (P.Message message) -> app.getBestEffortBroadcast().apply(message));
        abstractions.put("app.beb.pl", (P.Message message) -> app.getBestEffortBroadcast().getPerfectLink().apply(message));
        log.debug("App base registered");
        log.debug("Current abstraction map - {}", abstractions.keySet());
    }

    private void registerNewNNAtomicRegistry(String register) {
        String nnarAbstraction = String.format("app.nnar[%s]", register);
        NNAtomicRegistry registry = new NNAtomicRegistry(context, nnarAbstraction);

        abstractions.put(nnarAbstraction, (P.Message message) -> registry.apply(message));
        abstractions.put(nnarAbstraction + ".pl", (P.Message message) -> registry.getPerfectLink().apply(message));
        abstractions.put(nnarAbstraction + ".beb", (P.Message message) -> registry.getBroadcast().apply(message));
        abstractions.put(nnarAbstraction + ".beb.pl", (P.Message message) -> registry.getBroadcast().getPerfectLink().apply(message));

        log.info("New NNAR registered - {}", nnarAbstraction);
        log.debug("Current abstraction map - {}", abstractions.keySet());
    }

    private void registerNewUniformConsensus(String topic) {
        String ucAbstractionId = String.format("app.uc[%s]", topic);

        UniformConsensus consensus = new UniformConsensus(context, abstractions, ABSTRACTION_ID, topic);
        consensus.createNewEpochConsensus(EpochConsensusMessageUtils.createEPInternalState(-1, 0));

        abstractions.put(ucAbstractionId, (P.Message message) -> consensus.apply(message));
        abstractions.put(ucAbstractionId + ".ec", (P.Message message) -> consensus.getEpochChange().apply(message));
        abstractions.put(ucAbstractionId + ".ec.pl", (P.Message message) -> consensus.getEpochChange().getPerfectLink().apply(message));
        abstractions.put(ucAbstractionId + ".ec.beb", (P.Message message) -> consensus.getEpochChange().getBestEffortBroadcastUnit().apply(message));
        abstractions.put(ucAbstractionId + ".ec.beb.pl", (P.Message message) -> consensus.getEpochChange().getBestEffortBroadcastUnit().getPerfectLink().apply(message));
        abstractions.put(ucAbstractionId + ".ec.eld", (P.Message message) -> consensus.getEpochChange().getEventualLeaderDetector().apply(message));
        abstractions.put(ucAbstractionId + ".ec.eld.epfd", (P.Message message) -> consensus.getEpochChange().getEventualLeaderDetector().getEventuallyPerfectFailureDetector().apply(message));
        abstractions.put(ucAbstractionId + ".ec.eld.epfd.pl", (P.Message message) -> consensus.getEpochChange().getEventualLeaderDetector().getEventuallyPerfectFailureDetector().getPerfectLink().apply(message));

        log.info("New Uniform Consensus registered - {}", ucAbstractionId);
        log.debug("Current abstraction map - {}", abstractions.keySet());
    }
}