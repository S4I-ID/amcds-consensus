package client.threads;

import client.handlers.app.AppBase;
import client.handlers.point_link.PerfectLink;
import client.model.SystemContext;
import client.service.AbstractionService;

public class ThreadFactory {
    private static ThreadFactory instance;

    private ThreadFactory() {}

    public static ThreadFactory getInstance() {
        if (instance == null) {
            instance = new ThreadFactory();
        }
        return instance;
    }

    public NodeNetworkThread createNodeNetworkReaderThread(SystemContext systemContext, String baseAbstractionId) {
        PerfectLink perfectLink = new PerfectLink(systemContext, baseAbstractionId);
        return new NodeNetworkThread(systemContext.getMessageQueue(), perfectLink);
    }

    public SystemThread createSystemThread(SystemContext systemContext) {
        AppBase appBase = new AppBase(systemContext);
        AbstractionService abstractionService = new AbstractionService(systemContext);
        abstractionService.registerAbstractions(appBase);
        return new SystemThread(systemContext, abstractionService);
    }
}
