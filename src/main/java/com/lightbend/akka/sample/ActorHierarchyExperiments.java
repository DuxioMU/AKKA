package com.lightbend.akka.sample;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

/**
 * Actor 的层次结构：
 * 阿卡的演员总是属于父母。通常，您通过调用创建一个actor 。
 * 而不是创建一个“独立”的演员，这种注入新的演员作为一个
 * 孩子到现有的树：创作者的演员成为母公司新创建的儿童演员。
 * 你可能会问，谁是你创造的第一个演员的家长？getContext().actorOf()
 */
class PrintMyActorRefActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals("printit",p->{
                    ActorRef secondRef = getContext().actorOf(Props.empty(),"second-actor");
                    System.out.println("Second:" + secondRef);
                }).build();
    }
}
public class ActorHierarchyExperiments {
    public static void main(String[] args)throws java.io.IOException{
        ActorSystem system = ActorSystem.create("testSystem");
        ActorRef firstRef = system.actorOf(Props.create(PrintMyActorRefActor.class),"first-actor");
        System.out.println("First:"+firstRef);
        firstRef.tell("printit", ActorRef.noSender());

        System.out.println(">>> Press ENTER to exit <<<");
        try{
            System.in.read();
        }finally{
            system.terminate();
        }
    }
}
