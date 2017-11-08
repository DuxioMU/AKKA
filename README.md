# AKKA
定义Actor和消息
消息可以是任意类型（任何子类型Object）。可以发送盒装原始值（如String，Integer，Boolean作为消息等）以及纯数据结构如数组和集合类型。
Hello World演员使用三种不同的讯息：
  ● WhoToGreet：问候语的接收者
  ● Greet：执行问候的指示
  ● Greeting：包含问候语的消息
在定义Actor和他们的信息时，记住这些建议：
  ● 由于消息是Actor的公共API，所以定义具有良好名称和丰富的语义和领域特定含义的消息是一个好习惯，即使它们只是包装数据类型。这将使得使用，理解和调试基于角色的系统更容易。
  ● 消息应该是不可变的，因为它们是在不同的线程之间共享的。
  ● 将Actor的相关消息作为静态类放入Actor的类中是一个好习惯。这使得更容易理解参与者期望和处理的消息类型。
  ● props在描述如何构造Actor的Actor的类中使用静态方法也是一种常见的模式。
让我们看看Actor如何实现Greeter并Printer演示这些最佳实践。
更好的演员
下面的代码片段Greeter.java实现了GreeterActor：

package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.lightbend.akka.sample.Printer.Greeting;

public class Greeter extends AbstractActor {
  static public Props props(String message, ActorRef printerActor) {
    return Props.create(Greeter.class, () -> new Greeter(message, printerActor));
  }

  static public class WhoToGreet {
    public final String who;

    public WhoToGreet(String who) {
        this.who = who;
    }
  }

  static public class Greet {
    public Greet() {
    }
  }

  private final String message;
  private final ActorRef printerActor;
  private String greeting = "";

  public Greeter(String message, ActorRef printerActor) {
    this.message = message;
    this.printerActor = printerActor;
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(WhoToGreet.class, wtg -> {
          this.greeting = message + ", " + wtg.who;
        })
        .match(Greet.class, x -> {
          printerActor.tell(new Greeting(greeting), getSelf());
        })
        .build();
  }
}

让我们分解一下功能：
  ● 的Greeter类扩展akka.actor.AbstractActor类并实现createReceive方法。
  ● 所述Greeter构造函数接受两个参数：String message，这将构建问候消息时和使用ActorRef printerActor，这对演员处理问候的输出的参考。
  ● 该receiveBuilder定义的行为; Actor如何应对接收到的不同消息。演员可以有状态。由于受到Actor模型的保护，访问或变异Actor的内部状态是完全线程安全的。该createReceive方法应该处理actor期望的消息。在这种情况下Greeter，它期望两种类型的消息：WhoToGreet和Greet。前者将更新greetingActor 的状态，后者将触发发送greeting给PrinterActor。
  ● 该greeting变量包含Actor的状态，""默认设置为。
  ● 静态props方法创建并返回一个Props实例。Props是一个配置类，用于指定用于创建actor的选项，将其视为一个不可变的因此可自由共享的用于创建可包含相关的部署信息的actor的配方。这个例子只是传递Actor在构造时需要的参数。我们将props在本教程的后面看到这个方法。
打印机演员
该Printer实现非常简单：
  ● 它通过创建一个记录器Logging.getLogger(getContext().getSystem(), this);。通过这样做，我们可以log.info()在Actor中写入，而不需要任何额外的布线。
  ● 它只处理一种类型的消息Greeting，并记录该消息的内容。
  
package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Printer extends AbstractActor {
  static public Props props() {
    return Props.create(Printer.class, () -> new Printer());
  }

  static public class Greeting {
    public final String message;

    public Greeting(String message) {
      this.message = message;
    }
  }

  private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  public Printer() {
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(Greeting.class, greeting -> {
            log.info(greeting.message);
        })
        .build();
  }
}


创建演员
到目前为止，我们已经看过演员及其讯息的定义。现在让我们深入一下位置透明的力量，看看如何创建Actor实例。
位置透明的力量
在Akka中，您无法使用new关键字创建Actor的实例。相反，您使用工厂创建Actor实例。工厂不返回一个actor实例，而是一个akka.actor.ActorRef指向actor实例的引用。这种间接的方式为分布式系统增加了很多功能和灵活性。
在阿卡位置并不重要。位置透明度意味着ActorRefcan在保留相同语义的同时，代表正在运行的进程中或远程计算机上的实例。如果需要，运行时可以通过在运行时更改Actor的位置或整个应用程序拓扑来优化系统。这使得失败管理的“让它崩溃”模型成为可能，系统可以通过崩溃错误的Actors并重新启动健康的Actors来自我修复。
阿卡演员系统
这个akka.actor.ActorSystem工厂在某种程度上和Spring相似BeanFactory。它作为演员的容器并管理他们的生命周期。该actorOf工厂方法创建演员和采用两个参数，称为配置对象Props和一个名称。
演员和ActorSystem名字在阿卡重要。例如，您使用它们进行查找。使用与您的域模型一致的有意义的名称可以更容易地推断出他们的道路。
上一个主题回顾了Hello World Actor的定义。让我们看看AkkaQuickstart.java创建的文件Greeter和PrinterActor实例中的代码：

final ActorRef printerActor = 
  system.actorOf(Printer.props(), "printerActor");
final ActorRef howdyGreeter = 
  system.actorOf(Greeter.props("Howdy", printerActor), "howdyGreeter");
final ActorRef helloGreeter = 
  system.actorOf(Greeter.props("Hello", printerActor), "helloGreeter");
final ActorRef goodDayGreeter = 
  system.actorOf(Greeter.props("Good day", printerActor), "goodDayGreeter");
  
注意以下几点：
  ● 创建Actor 的actorOf方法。正如我们在上一个主题中讨论的那样，它使用类的静态方法来获取值。提供对新创建的Actor实例的引用。ActorSystemPrinterpropsPrinterPropsActorRefPrinter
  ● 因为Greeter代码创建了三个Actor实例，每个实例都有一个特定的问候消息。
注意：在这个例子中，GreeterActor都使用相同的Printer实例，但是我们可以创建PrinterActor的多个实例。该示例使用一个来说明消息传递的一个重要概念，我们稍后会介绍。
接下来，让我们看看如何与演员沟通。


异步通信
参与者是被动的和消息驱动的。演员在收到消息之前不会做任何事情。参与者使用异步消息进行通信。这确保了发送者不会一直等待他们的消息被接收者处理。相反，发件人将邮件放在收件人的邮箱中，可以自由地进行其他工作。Actor的邮箱本质上是一个带有排序语义的消息队列。保留从同一个Actor发送的多个消息的顺序，但可以与另一个Actor发送的消息交织。
你可能想知道当Actor没有处理消息的时候正在做什么，即做实际的工作？它处于暂停状态，除了内存之外，它不消耗任何资源。再一次显示演员的轻量级，高效性。
发送消息给一个Actor
为了把消息到演员的邮箱，使用tell的方法ActorRef。例如，Hello World的主要类将消息发送给GreeterActor，如下所示：

howdyGreeter.tell(new WhoToGreet("Akka"), ActorRef.noSender());
howdyGreeter.tell(new Greet(), ActorRef.noSender());

howdyGreeter.tell(new WhoToGreet("Lightbend"), ActorRef.noSender());
howdyGreeter.tell(new Greet(), ActorRef.noSender());

helloGreeter.tell(new WhoToGreet("Java"), ActorRef.noSender());
helloGreeter.tell(new Greet(), ActorRef.noSender());

goodDayGreeter.tell(new WhoToGreet("Play"), ActorRef.noSender());
goodDayGreeter.tell(new Greet(), ActorRef.noSender());

该Greeter演员也将消息发送给Printer演员：

printerActor.tell(new Greeting(greeting), getSelf());

我们已经看过如何创建演员和发送消息。现在，我们Main来看整个课程。


主类
MainHello World中的类创建并控制actor。注意使用ActorSystem一个容器和actorOf创建Actor 的方法。最后，这个类创建发送给Actor的消息。

package com.lightbend.akka.sample;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.lightbend.akka.sample.Greeter.*;

import java.io.IOException;

public class AkkaQuickstart {
  public static void main(String[] args) {
    final ActorSystem system = ActorSystem.create("helloakka");
    try {
      final ActorRef printerActor = 
        system.actorOf(Printer.props(), "printerActor");
      final ActorRef howdyGreeter = 
        system.actorOf(Greeter.props("Howdy", printerActor), "howdyGreeter");
      final ActorRef helloGreeter = 
        system.actorOf(Greeter.props("Hello", printerActor), "helloGreeter");
      final ActorRef goodDayGreeter = 
        system.actorOf(Greeter.props("Good day", printerActor), "goodDayGreeter");

      howdyGreeter.tell(new WhoToGreet("Akka"), ActorRef.noSender());
      howdyGreeter.tell(new Greet(), ActorRef.noSender());

      howdyGreeter.tell(new WhoToGreet("Lightbend"), ActorRef.noSender());
      howdyGreeter.tell(new Greet(), ActorRef.noSender());

      helloGreeter.tell(new WhoToGreet("Java"), ActorRef.noSender());
      helloGreeter.tell(new Greet(), ActorRef.noSender());

      goodDayGreeter.tell(new WhoToGreet("Play"), ActorRef.noSender());
      goodDayGreeter.tell(new Greet(), ActorRef.noSender());

      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } catch (IOException ioe) {
    } finally {
      system.terminate();
    }
  }
}

同样，我们再来看一下定义Actor和他们接受的消息的完整源代码。


完整的示例代码
下面是三个类的完整源代码Greeter，Printer并AkkaQuickstart创建示例应用程序：
Greeter.java
package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.lightbend.akka.sample.Printer.Greeting;

public class Greeter extends AbstractActor {
  static public Props props(String message, ActorRef printerActor) {
    return Props.create(Greeter.class, () -> new Greeter(message, printerActor));
  }

  static public class WhoToGreet {
    public final String who;

    public WhoToGreet(String who) {
        this.who = who;
    }
  }

  static public class Greet {
    public Greet() {
    }
  }

  private final String message;
  private final ActorRef printerActor;
  private String greeting = "";

  public Greeter(String message, ActorRef printerActor) {
    this.message = message;
    this.printerActor = printerActor;
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(WhoToGreet.class, wtg -> {
          this.greeting = message + ", " + wtg.who;
        })
        .match(Greet.class, x -> {
          printerActor.tell(new Greeting(greeting), getSelf());
        })
        .build();
  }
}
Printer.java
package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Printer extends AbstractActor {
  static public Props props() {
    return Props.create(Printer.class, () -> new Printer());
  }

  static public class Greeting {
    public final String message;

    public Greeting(String message) {
      this.message = message;
    }
  }

  private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  public Printer() {
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(Greeting.class, greeting -> {
            log.info(greeting.message);
        })
        .build();
  }
}
AkkaQuickstart.java
package com.lightbend.akka.sample;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.lightbend.akka.sample.Greeter.*;

import java.io.IOException;

public class AkkaQuickstart {
  public static void main(String[] args) {
    final ActorSystem system = ActorSystem.create("helloakka");
    try {
      final ActorRef printerActor = 
        system.actorOf(Printer.props(), "printerActor");
      final ActorRef howdyGreeter = 
        system.actorOf(Greeter.props("Howdy", printerActor), "howdyGreeter");
      final ActorRef helloGreeter = 
        system.actorOf(Greeter.props("Hello", printerActor), "helloGreeter");
      final ActorRef goodDayGreeter = 
        system.actorOf(Greeter.props("Good day", printerActor), "goodDayGreeter");

      howdyGreeter.tell(new WhoToGreet("Akka"), ActorRef.noSender());
      howdyGreeter.tell(new Greet(), ActorRef.noSender());

      howdyGreeter.tell(new WhoToGreet("Lightbend"), ActorRef.noSender());
      howdyGreeter.tell(new Greet(), ActorRef.noSender());

      helloGreeter.tell(new WhoToGreet("Java"), ActorRef.noSender());
      helloGreeter.tell(new Greet(), ActorRef.noSender());

      goodDayGreeter.tell(new WhoToGreet("Play"), ActorRef.noSender());
      goodDayGreeter.tell(new Greet(), ActorRef.noSender());

      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } catch (IOException ioe) {
    } finally {
      system.terminate();
    }
  }
}
作为另一个最佳实践，我们应该提供一些测试覆盖


测试演员
Hello World示例中的测试说明了如何使用JUnit框架。测试覆盖不完整。它只是显示测试actor代码是多么容易，并提供了一些基本的概念。你可以添加它作为一个练习来增加自己的知识。
测试类正在使用akka.test.javadsl.TestKit，这是一个演员和演员系统集成测试的模块。这个类只使用TestKit提供的一小部分功能。
集成测试可以帮助我们确保演员的行为是异步的。这第一个测试使用TestKit探针来询问和验证预期的行为。我们来看一个源代码片段：

package com.lightbend.akka.sample;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.lightbend.akka.sample.Greeter.*;
import com.lightbend.akka.sample.Printer.*;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class AkkaQuickstartTest {
    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testGreeterActorSendingOfGreeting() {
        final TestKit testProbe = new TestKit(system);
        final ActorRef helloGreeter = system.actorOf(Greeter.props("Hello", testProbe.getRef()));
        helloGreeter.tell(new WhoToGreet("Akka"), ActorRef.noSender());
        helloGreeter.tell(new Greet(), ActorRef.noSender());
        Greeting greeting = testProbe.expectMsgClass(Greeting.class);
        assertEquals("Hello, Akka", greeting.message);
    }
}

一旦我们有一个引用TestKit探测，我们将ActorRef其Greeter作为构造函数参数的一部分传递给它。之后，我们发送了两封邮件Greeter; 一个设置问候者迎接，另一个触发发送Greeting。在expectMsg对方法TestKit的消息是否得到了发送验证。
示例代码只是划伤了可用功能的表面TestKit。一个完整的概述可以在这里找到。
现在我们已经审查了所有的代码。让我们再次运行该示例并查看其输出。

运行应用程序
您可以从命令行或IDE运行Hello World应用程序。本指南的最后一个主题描述了如何从IntelliJ IDEA运行它。但是，在我们再次运行应用程序之前，让我们快速浏览构建文件。
构建文件
如下所示，此示例项目中使用的Maven（pom.xml）和Gradle（build.gradle）的构建文件非常简单。有关更多信息，请参阅您选择的构建工具的文档。
Maven的

<project>
    <modelVersion>4.0.0</modelVersion>

    <groupId>hello-akka-java</groupId>
    <artifactId>app</artifactId>
    <version>1.0</version>

    <dependencies>
        <dependency>
            <groupId>com.typesafe.akka</groupId>
            <artifactId>akka-actor_2.12</artifactId>
            <version>2.5.3</version>
        </dependency>
        <dependency>
            <groupId>com.typesafe.akka</groupId>
            <artifactId>akka-testkit_2.12</artifactId>
            <version>2.5.3</version>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.5.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>1.6.0</version>
                <configuration>
                    <executable>java</executable>
                    <arguments>
                        <argument>-classpath</argument>
                        <classpath />
                        <argument>com.lightbend.akka.sample.AkkaQuickstart</argument>
                    </arguments>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>


运行该项目
就像您之前所做的那样，从控制台运行应用程序：

Maven
$ mvn compile exec:exec

摇篮
输出应该的东西像这样（所有的方式滚动看到演员输出正确的）：

Maven
[INFO] Scanning for projects...
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building app 1.0
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- exec-maven-plugin:1.6.0:exec (default-cli) @ app ---
>>> Press ENTER to exit <<<
[INFO] [05/11/2017 14:07:20.790] [helloakka-akka.actor.default-dispatcher-2] [akka://helloakka/user/printerActor] Hello, Java
[INFO] [05/11/2017 14:07:20.791] [helloakka-akka.actor.default-dispatcher-2] [akka://helloakka/user/printerActor] Good day, Play
[INFO] [05/11/2017 14:07:20.791] [helloakka-akka.actor.default-dispatcher-2] [akka://helloakka/user/printerActor] Howdy, Akka
[INFO] [05/11/2017 14:07:20.791] [helloakka-akka.actor.default-dispatcher-2] [akka://helloakka/user/printerActor] Howdy, Lightbend



请记住，我们设置我们的Printer演员使用阿卡的记录器？这就是为什么我们记录事情时有很多额外的信息。日志输出包含诸如何时和从哪个参与者记录的信息。让我们专注于Printer演员的输出一段时间：

... Howdy, Akka
... Hello, Java
... Good day, Play
... Howdy, Lightbend

这是我们将代码发送给GreeterActor的结果：

howdyGreeter.tell(new WhoToGreet("Akka"), ActorRef.noSender());
howdyGreeter.tell(new Greet(), ActorRef.noSender());

howdyGreeter.tell(new WhoToGreet("Lightbend"), ActorRef.noSender());
howdyGreeter.tell(new Greet(), ActorRef.noSender());

helloGreeter.tell(new WhoToGreet("Java"), ActorRef.noSender());
helloGreeter.tell(new Greet(), ActorRef.noSender());

goodDayGreeter.tell(new WhoToGreet("Play"), ActorRef.noSender());
goodDayGreeter.tell(new Greet(), ActorRef.noSender());

要运行测试，请输入test任务：
Maven
$ mvn test


尝试运行代码几次，并确保注意到日志的顺序。你有没有注意到它可以从一个跑到另一个跑？这里发生了什么？异步行为变得明显。这可能是一个新的心理模型给你。但是，一旦你获得了经验，一切都将变得清晰。就像Matrix的Neo一样。
下一步
如果您使用IntelliJ，请尝试将示例项目与IntelliJ IDEA集成。
要继续学习Akka和Actor Systems的更多信息，请看下面的入门指南。快乐的徒步旅行！


IntelliJ IDEA
JetBrains中的IntelliJ是Java / Scala社区中领先的IDE之一，它对Akka有着出色的支持。本部分将指导您设置，测试和运行示例项目。
建立项目
建立这个项目很简单。打开IntelliJ并选择File -> Open...并指向您安装示例项目的目录。
检查项目代码
如果我们打开文件，src/main/java/com/lightbend/akka/sample/HelloAkka.java我们会看到很多以行开头的行//# ...。这些行用作本文档的指令。为了摆脱源代码中的这些行，我们可以利用IntelliJ中的真棒查找/替换功能。选择Edit -> Find -> Replace in Path...。选中该Regex框并添加以下正则表达式[//#].*，然后单击Replace in Find Window...。选择以取代所有的事件，瞧，这些线路消失了！重复所有要删除注释的文件。
测试和运行
为了测试，我们只需右键单击该文件src/test/java/com/lightbend/akka/sample/HelloAkkaTest.java并选择Run 'HelloAkkaTest'。
与运行应用程序类似，我们右键单击该文件src/main/java/com/lightbend/akka/sample/HelloAkka.java并选择Run 'HelloAkka.main()'
有关更多详细信息，请参阅运行应用程序部分。

