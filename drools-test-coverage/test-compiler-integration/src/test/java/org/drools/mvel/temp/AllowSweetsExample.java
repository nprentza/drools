package org.drools.mvel.temp;

import org.drools.core.BeliefSystemType;
import org.drools.core.ClassObjectFilter;
import org.drools.core.SessionConfiguration;
import org.drools.core.beliefsystem.BeliefSystem;
import org.drools.core.beliefsystem.jtms.UpdateRestoreCommand;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.impl.KnowledgeBaseFactory;
import org.drools.serialization.protobuf.ProtobufMessages;
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
            "    insertLogical( new DenySweets(p) ); \n" +
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

    private String drl3 = "package org.drools.mvel.temp; \n " +
            "import " + Person.class.getCanonicalName() + "; \n"  +
            "import " + AllowSweets.class.getCanonicalName() + "; \n"  +
            "import " + UpdateRestoreCommand.class.getCanonicalName() + "; \n"  +
            "import " + Map.class.getCanonicalName() + "; \n" +
            "import " + HashMap.class.getCanonicalName() + "; \n" +
            "import " + Request.class.getCanonicalName() + "; \n" +
            " " +
            "rule AllowAll when " +
            "     p : Person() \n" +
            "then " +
            "    Map<String,Object> m = new HashMap<>(); \n " +
            "       m.put(\"allowDenySweets\",\"allow\"); \n " +
            "    insertLogical( new UpdateRestoreCommand(kcontext.getKieRuntime().getFactHandle(p), m) ); \n " +
            "end " +
            "rule OnDietSuprise when " +
            "     String( this == 'rule_OnDietSuprise') \n" +
            "     p : Person() \n" +
            "then " +
            "    Map<String,Object> m = new HashMap<>(); \n " +
            "       m.put(\"cheatDay\",\"Thursday\"); \n " +
            "    insertLogical( new UpdateRestoreCommand(kcontext.getKieRuntime().getFactHandle(p), m)); \n " +
            "end " +
            "rule DoNotAllowOnDiet when " +
            "    p : Person( onDiet == 'True' ) \n" +
            "then " +
            "    Map<String,Object> m = new HashMap<>(); \n " +
            "       m.put(\"allowDenySweets\",\"allow\"); \n " +
            "    insertLogical( new UpdateRestoreCommand(kcontext.getKieRuntime().getFactHandle(p), m), \"neg\" ); \n " +
            "end " +
            "\n";
    private String drl4_test = "package org.drools.mvel.temp; \n " +
            "import " + Person.class.getCanonicalName() + "; \n"  +
            "import " + AllowSweets.class.getCanonicalName() + "; \n"  +
            "import " + UpdateRestoreCommand.class.getCanonicalName() + "; \n"  +
            "import " + Map.class.getCanonicalName() + "; \n" +
            "import " + HashMap.class.getCanonicalName() + "; \n" +
            "import " + Request.class.getCanonicalName() + "; \n" +
            " " +
            "rule AllowAll when " +
            "     String( this == 'rule_AllowAll') \n" +
            "     r : Request() \n" +
            "then " +
            "    Map<String,Object> m = new HashMap<>(); \n " +
            "       m.put(\"allowDenySweets\",\"allow\"); \n " +
            "    insertLogical( new UpdateRestoreCommand(kcontext.getKieRuntime().getFactHandle(r.getPerson()), m) ); \n " +
            "end " +
            "rule OnDietSuprise when " +
            "     String( this == 'rule_OnDietSuprise') \n" +
            "     r : Request() \n" +
            "then " +
            "    Map<String,Object> m = new HashMap<>(); \n " +
            "       m.put(\"cheatDay\",\"Thursday\"); \n " +
            "    insertLogical( new UpdateRestoreCommand(kcontext.getKieRuntime().getFactHandle(r.getPerson()), m)); \n " +
            "end " +
            "rule DontAllowOnDiet when " +
            "     String( this == 'rule_DontAllowOnDiet') \n" +
            "     r : Request() \n" +
            "then " +
            "    Map<String,Object> m = new HashMap<>(); \n " +
            "       m.put(\"allowDenySweets\",\"allow\"); \n " +
            "    insertLogical( new UpdateRestoreCommand(kcontext.getKieRuntime().getFactHandle(r.getPerson()), m), \"neg\"); \n " +
            "end " +
            "rule AllowOnCheatDay when " +
            "     String( this == 'rule_AllowOnCheatDay') \n" +
            "     r : Request() \n" +
            "then " +
            "    Map<String,Object> m = new HashMap<>(); \n " +
            "       m.put(\"allowDenySweets\",\"allow2\"); \n " +
            "    insertLogical( new UpdateRestoreCommand(kcontext.getKieRuntime().getFactHandle(r.getPerson()), m)); \n " +
            "end " +
            "rule OnDietSuprise2 when " +
            "     String( this == 'rule_OnDietSuprise2') \n" +
            "     r : Request() \n" +
            "then " +
            "    Map<String,Object> m = new HashMap<>(); \n " +
            "       m.put(\"cheatDay\",\"Monday\"); \n " +
            "    insertLogical( new UpdateRestoreCommand(kcontext.getKieRuntime().getFactHandle(r.getPerson()), m)); \n " +
            "end " +
            "\n";

    private String drl5_test = "package org.drools.mvel.temp; \n " +
            "import " + Person.class.getCanonicalName() + "; \n"  +
            "import " + AllowSweets.class.getCanonicalName() + "; \n"  +
            "import " + UpdateRestoreCommand.class.getCanonicalName() + "; \n"  +
            "import " + Map.class.getCanonicalName() + "; \n" +
            "import " + HashMap.class.getCanonicalName() + "; \n" +
            "import " + Request.class.getCanonicalName() + "; \n" +
            " " +
            "rule Rule_1 when " +
            "     String( this == 'rule_1') \n" +
            "     r : Request() \n" +
            "then " +
            "    Map<String,Object> m = new HashMap<>(); \n " +
            "       m.put(\"allowDenySweets\",\"allow\"); \n " +
            "       m.put(\"cheatDay\",\"Monday\"); \n " +
            "    insertLogical( new UpdateRestoreCommand(kcontext.getKieRuntime().getFactHandle(r.getPerson()), m) ); \n " +
            "end " +
            "rule Rule_2 when " +
            "     String( this == 'rule_2') \n" +
            "     r : Request() \n" +
            "then " +
            "    Map<String,Object> m = new HashMap<>(); \n " +
            "       m.put(\"cheatDay\",\"Thursday\"); \n " +
            "    insertLogical( new UpdateRestoreCommand(kcontext.getKieRuntime().getFactHandle(r.getPerson()), m)); \n " +
            "end " +
            "rule Rule_3 when " +
            "     String( this == 'rule_3') \n" +
            "     r : Request() \n" +
            "then " +
            "    Map<String,Object> m = new HashMap<>(); \n " +
            "       m.put(\"allowDenySweets\",\"allow\"); \n " +
            "    insertLogical( new UpdateRestoreCommand(kcontext.getKieRuntime().getFactHandle(r.getPerson()), m), \"neg\"); \n " +
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

    @Test
    public void testScenario_3(){
        KieSession kSession = getSessionFromString( this.drl3 );

        Person mary = new Person("Mary", "True");
        FactHandle fh_mary = kSession.insert(mary);
        FactHandle fh_rule2 = kSession.insert("rule_OnDietSuprise");
        kSession.fireAllRules();
        //Collection cObjects = kSession.getObjects( new ClassObjectFilter( UpdateRestoreCommand.class ) );
        //Assert.assertEquals(1, cObjects.size());
    }

    @Test
    public void testScenario_4(){
        KieSession kSession = getSessionFromString( this.drl4_test );

        Person mary = new Person("Mary", "True");
        FactHandle fh_mary = kSession.insert(mary);
        Request abc = new Request(mary);
        kSession.insert(abc);
        FactHandle fh_rule1 = kSession.insert("rule_AllowAll");
        kSession.fireAllRules();
        Collection cObjects = kSession.getObjects( new ClassObjectFilter( UpdateRestoreCommand.class ) );
        //Assert.assertEquals(1, cObjects.size());
        UpdateRestoreCommand obj=  (UpdateRestoreCommand) cObjects.toArray()[0];
        Assert.assertEquals("allow",((Person)obj.getFactHandle().getObject()).getAllowDenySweets());

        FactHandle fh_rule2 = kSession.insert("rule_OnDietSuprise");
        FactHandle fh_rule3 = kSession.insert("rule_DontAllowOnDiet");
        kSession.fireAllRules();
        cObjects = kSession.getObjects( new ClassObjectFilter( UpdateRestoreCommand.class ) );
       // Assert.assertEquals(1, cObjects.size());
        obj = (UpdateRestoreCommand) cObjects.toArray()[0];
        // mary.allowDenySweets is expected to be "tbd",
        Assert.assertEquals("tbd",((Person)obj.getFactHandle().getObject()).getAllowDenySweets());

        FactHandle fh_rule4 = kSession.insert("rule_AllowOnCheatDay");
        kSession.fireAllRules();
        cObjects = kSession.getObjects( new ClassObjectFilter( UpdateRestoreCommand.class ) );
        obj = (UpdateRestoreCommand) cObjects.toArray()[0];
        // mary.allowDenySweets is expected to be "tbd",
        Assert.assertEquals("tbd",((Person)obj.getFactHandle().getObject()).getAllowDenySweets());

        kSession.delete(fh_rule3);
        kSession.fireAllRules();
        cObjects = kSession.getObjects( new ClassObjectFilter( UpdateRestoreCommand.class ) );
        obj = (UpdateRestoreCommand) cObjects.toArray()[0];
        // mary.allowDenySweets is expected to be "allow",
        Assert.assertEquals("allow2",((Person)obj.getFactHandle().getObject()).getAllowDenySweets());
    }

    @Test
    public void testScenario_5(){
        KieSession kSession = getSessionFromString( this.drl5_test );

        Person mary = new Person("Mary", "True");
        FactHandle fh_mary = kSession.insert(mary);
        Request abc = new Request(mary);
        kSession.insert(abc);
        FactHandle fh_rule1 = kSession.insert("rule_1");
        kSession.fireAllRules();
        Collection cObjects = kSession.getObjects( new ClassObjectFilter( UpdateRestoreCommand.class ) );
        UpdateRestoreCommand obj=  (UpdateRestoreCommand) cObjects.toArray()[0];
        Assert.assertEquals("allow",((Person)obj.getFactHandle().getObject()).getAllowDenySweets());
        Assert.assertEquals("Monday",((Person)obj.getFactHandle().getObject()).getCheatDay());

        FactHandle fh_rule2 = kSession.insert("rule_2");
        kSession.fireAllRules();
        cObjects = kSession.getObjects( new ClassObjectFilter( UpdateRestoreCommand.class ) );
        obj=  (UpdateRestoreCommand) cObjects.toArray()[0];
        Assert.assertEquals("Thursday",((Person)obj.getFactHandle().getObject()).getCheatDay());

        FactHandle fh_rule3 = kSession.insert("rule_3");
        kSession.fireAllRules();
        cObjects = kSession.getObjects( new ClassObjectFilter( UpdateRestoreCommand.class ) );
        obj=  (UpdateRestoreCommand) cObjects.toArray()[0];
        Assert.assertEquals("tbd",((Person)obj.getFactHandle().getObject()).getAllowDenySweets());
        Assert.assertEquals("tbd",((Person)obj.getFactHandle().getObject()).getCheatDay());

        kSession.delete(fh_rule3);
        kSession.fireAllRules();
        cObjects = kSession.getObjects( new ClassObjectFilter( UpdateRestoreCommand.class ) );
        obj = (UpdateRestoreCommand) cObjects.toArray()[0];
        Assert.assertEquals("allow",((Person)obj.getFactHandle().getObject()).getAllowDenySweets());
        Assert.assertEquals("Monday",((Person)obj.getFactHandle().getObject()).getCheatDay());
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

    public static class Request{
        public Person p;

        public Request(Person p) {
            this.p = p;
        }

        public Person getPerson() {
            return this.p;
        }
    }

}
