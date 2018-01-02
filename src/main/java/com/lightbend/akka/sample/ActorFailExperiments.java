package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

/**
 * 父母和孩子在整个生命周期中都有联系。
 * 每当一个actor失败（抛出一个异常或一个
 * 未处理的异常从中冒出来receive），它就暂
 * 时被挂起。如前所述，故障信息传播给父节点，
 * 父节点然后决定如何处理由该子节点引起的异常
 * 。这样，父母就像他们的孩子一样负责监督。默认
 * 的主管策略是停止并重新启动孩子。
 * 如果不更改默认策略，则所有失败都会导致重新启动
 */

class SupervisingActor extends AbstractActor {
    ActorRef child = getContext().actorOf(Props.create(SupervisedActor.class), "supervised-actor");

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals("failChild", f -> {
                    child.tell("fail", getSelf());
                })
                .build();
    }
}
class SupervisedActor extends AbstractActor {
    @Override
    public void preStart() {
        System.out.println("supervised actor started");
    }

    @Override
    public void postStop() {
        System.out.println("supervised actor stopped");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals("fail", f -> {
                    System.out.println("supervised actor fails now");
                    throw new Exception("I failed!");
                })
                .build();
    }
}
public class ActorFailExperiments  {
    public static void main(String[] args) {
        //失败后，受监督的演员被停止并立即重新启动
        ActorSystem system = ActorSystem.create("fail");
        ActorRef supervisingActor = system.actorOf(Props.create(SupervisingActor.class), "supervising-actor");
        supervisingActor.tell("failChild", ActorRef.noSender());
    }
}
