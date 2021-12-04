package org.drools.mvel.temp;

import org.drools.core.BeliefSystemType;
import org.drools.core.ClassObjectFilter;
import org.drools.core.SessionConfiguration;
import org.drools.core.beliefsystem.BeliefSystem;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.impl.KnowledgeBaseFactory;
import org.junit.Assert;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class AllowSweetsExample {

    private String drl1 = "package org.drools.mvel.temp; \n " +
            "import " + Person.class.getCanonicalName() + "; \n"  +
            "import " + AllowSweets.class.getCanonicalName() + "; \n"  +
            " " +
            "rule AllowAll when " +
            "     p : Person() \n" +
            "then " +
            "    System.out.println(\"Rule: AllowAll for \" + p.getName()); \n" +
            "    insertLogical( new AllowSweets(p) ); \n " +
            "end " +
            "rule DoNotAllowOnDiet when " +
            "    p : Person( onDiet == 'True' ) \n" +
            "then " +
            "    System.out.println(\"Rule: DoNotAllowOnDiet for \" + p.getName()); \n" +
            "    insertLogical( new AllowSweets(p), \"neg\" ); \n " +
            "end " +
            "rule AllowOnSaturday when " +
            "    String( this == 'Saturday') \n" +
            "    p : Person( onDiet == 'True' ) \n" +
            "then " +
            "    System.out.println(\"Rule: AllowOnSaturday for \" + p.getName()); \n" +
            "    insertLogical( new AllowSweets(p) ); \n " +
            "end " +
            "\n";

    private String drl2 = "package org.drools.mvel.temp; \n " +
            "import " + Person.class.getCanonicalName() + "; \n"  +
            "import " + AllowSweets.class.getCanonicalName() + "; \n"  +
            " " +
            "rule DoNotAllowOnDiet when " +
            "    p : Person( onDiet == 'True' ) \n" +
            "then " +
            "    System.out.println(\"Rule: DoNotAllowOnDiet for \" + p.getName()); \n" +
            "    insertLogical( new AllowSweets(p), \"neg\" ); \n " +
            "end " +
            "rule AllowOnSaturday when " +
            "    String( this == 'Saturday') \n" +
            "    p : Person( onDiet == 'True' ) \n" +
            "then " +
            "    System.out.println(\"Rule: AllowOnSaturday for \" + p.getName()); \n" +
            "    insertLogical( new AllowSweets(p) ); \n " +
            "end " +
            "rule AllowAll when " +
            "     p : Person() \n" +
            "then " +
            "    System.out.println(\"Rule: AllowAll for \" + p.getName()); \n" +
            "    insertLogical( new AllowSweets(p) ); \n " +
            "end " +
            "\n";
    @Test
    public void testScenario_1(){
        KieSession kSession = getSessionFromString( this.drl1 );

        Person mary = new Person("Mary", "True");
        FactHandle fh_mary = kSession.insert(mary);
        kSession.fireAllRules();
        // we expect no object of type AllowSweets in the working memory:
        //  the 1st rule ALlowAll logically inserts an AllowSweets object
        //  but then the 2nd rule DoNotAllowOnDiet, activates a negative insertion of the same object
        //  and as a result the object is removed from the wm by the JTMSBeliefSystem.processBeliefSet method
        Collection cObjects = kSession.getObjects( new ClassObjectFilter( AllowSweets.class ) );
        Assert.assertEquals(0, cObjects.size());
        //pause();

        FactHandle fh_rule1 = kSession.insert("Saturday");
        kSession.fireAllRules();
        cObjects = kSession.getObjects( new ClassObjectFilter( AllowSweets.class ) );
        //  while "Saturday" is inserted which fires the AllowOnSaturday rule
        //  the activation of that rule insertLogical (new AllowSweets) is not executed, the object is not inserted into the wm
        Assert.assertEquals(1,cObjects.size());
    }

    @Test
    public void testScenario_2(){
        KieSession kSession = getSessionFromString( this.drl2 );

        Person mary = new Person("Mary", "True");
        FactHandle fh_mary = kSession.insert(mary);
        FactHandle fh_rule1 = kSession.insert("Saturday");

        kSession.fireAllRules();
        //  the order of rules in the drl2 is DoNotAllowOnDiet, AllowOnSaturday, AllowAll
        //  the first rule activates a negative logical insertion, while the 2nd and the 3rd activate a positive logical insertion
        //      DoNotAllowOnDiet activates a negative logical insertion of object AllowSweets, the object is inserted into the wp
        //   then, AllowOnSaturday activates a positive logical insertion of object AllowSweets,
        //          the JTMSBeliefSystem identifies a contradiction and the AllowSweets object is removed from the wm
        //   then, AllowALl activates a positive logical insertion of object AllowSweets but the object is not inserted in to the wm
        Collection cObjects = kSession.getObjects( new ClassObjectFilter( AllowSweets.class ) );
        Assert.assertEquals(1, cObjects.size());
    }

    public static void pause() {
        System.out.println( "Pressure enter to continue" );
        Scanner keyboard = new Scanner(System.in);
        keyboard.nextLine();
    }

    private KieSession getSessionFromString( String drl ) {
        KnowledgeBuilder knowledgeBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        knowledgeBuilder.add( ResourceFactory.newByteArrayResource( drl.getBytes() ),
                ResourceType.DRL );
        if (knowledgeBuilder.hasErrors()) {
            throw new RuntimeException( knowledgeBuilder.getErrors().toString() );
        }

        InternalKnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addPackages( knowledgeBuilder.getKnowledgePackages() );

        KieSessionConfiguration ksConf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        ((SessionConfiguration) ksConf).setBeliefSystemType(  BeliefSystemType.JTMS );

        KieSession session = kbase.newKieSession(ksConf, null);
        return session;
    }

}
