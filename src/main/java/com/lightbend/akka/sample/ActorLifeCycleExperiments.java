package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

/**
 * preStart() 在actor启动之后但在处理第一条消息之前被调用。
 postStop()在演员停下来之前被调用。在这之后没有消息被处理。
 */
class StartStopActor1 extends AbstractActor {


    @Override
    public void preStart() throws Exception {
        System.out.println("first started");
        getContext().actorOf(Props.create(StartStopActor2.class),"second");
    }

    @Override
    public void postStop() throws Exception {
        System.out.println("first stopped");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().matchEquals("stop",s->{
            getContext().stop(getSelf());
        }).build();
    }
}
class StartStopActor2 extends AbstractActor{

    @Override
    public void preStart() throws Exception {
        System.out.println("second started");
    }

    @Override
    public void postStop() throws Exception {
        System.out.println("second stopped");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .build();
    }
}
public class ActorLifeCycleExperiments {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("lifeCycle");
        ActorRef first = system.actorOf(Props.create(StartStopActor1.class),"first");
        first.tell("stop",ActorRef.noSender());
    }
}
