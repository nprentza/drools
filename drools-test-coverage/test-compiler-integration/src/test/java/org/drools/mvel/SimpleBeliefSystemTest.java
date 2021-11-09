package org.drools.mvel;

import org.drools.core.beliefsystem.simple.Memento;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.impl.KnowledgeBaseFactory;
import org.junit.Assert;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SimpleBeliefSystemTest {
    private String drl = "package org.drools.mvel; \n " +
            "import " + BankAccount.class.getCanonicalName() + "; \n"  +
            "import " + Memento.class.getCanonicalName() + "; \n"  +
            "import " + Map.class.getCanonicalName() + "; \n" +
            "import " + HashMap.class.getCanonicalName() + "; \n" +
            "import " + Request.class.getCanonicalName() + "; \n" +
            " " +
            "rule rule1 when " +
            "     String( this == 'rule1') \n" +
            "     r : Request()" +
            "then " +
            "    System.out.println(\"rule 1\"); \n" +
            "   Map<String,Object> m = new HashMap<>(); \n " +
            "       m.put(\"balance\",(float)100.0); \n " +
            "    insertLogical( new Memento(kcontext.getKieRuntime().getFactHandle(r.getBa()), m) ); \n " +
            "end " +
            "rule rule2 when " +
            "     String( this == 'rule2') \n" +
            "     r : Request()" +
            "then " +
            "    System.out.println(\"rule 2\"); \n" +
            "   Map<String,Object> m = new HashMap<>(); \n " +
            "       m.put(\"balance\",(float)99.0); \n " +
            "    insertLogical( new Memento(kcontext.getKieRuntime().getFactHandle(r.getBa()), m) ); \n " +
            "end " +
            "rule rule3 when " +
            "     String( this == 'rule3') \n" +
            "     r : Request()" +
            "then " +
            "    System.out.println(\"rule 3\"); \n" +
            "   Map<String,Object> m = new HashMap<>(); \n " +
            "       m.put(\"balance\",(float)50.0); \n " +
            "    insertLogical( new Memento(kcontext.getKieRuntime().getFactHandle(r.getBa()), m) ); \n " +
            "end " +
            "\n";

    // basic testing
    public void testScenario_0(String drl){
        KieSession kSession = getSessionFromString( drl );

        BankAccount baAbc = new BankAccount("123456789","Account Abc",0);

        FactHandle fh_baAbc = kSession.insert(baAbc);
        FactHandle fh_rule1 = kSession.insert("rule1");
        FactHandle fh_rule2 = kSession.insert("rule2");
        FactHandle fh_rule3 = kSession.insert("rule3");

        Request abc = new Request(baAbc);
        kSession.insert(abc);

        try{
            Assert.assertEquals("Initial balance is not 0, as expected.\n",0,(float)baAbc.getBalance(),0.0);
        }catch (AssertionError e){
            System.out.println(e.getMessage());
        }
        kSession.fireAllRules();
        try{
            Assert.assertEquals ("Balance after adding rule1 is not 100, as expected.\n",100f, baAbc.getBalance(),0.0);
        }catch (AssertionError e){
            System.out.println(e.getMessage());
        }
        //FactHandle fh2 = kSession.insert("rule2");
        kSession.fireAllRules();
        try{
            Assert.assertEquals("Balance changed after adding rule2, is not 100 as expected.\n",100f, (float)baAbc.getBalance(), 0.0);
        }catch (AssertionError e){
            System.out.println(e.getMessage());
        }
        kSession.delete(fh_rule1);
        kSession.fireAllRules();
        try {
            Assert.assertEquals("Balance did not change to 99, after deleting rule1, as expected.\n",99f, (float) baAbc.getBalance(), 0.0);
        }catch (AssertionError e){
            System.out.println(e.getMessage());
        }

        System.out.println("Bank account summary: " + baAbc.accountSummary());
    }

    // 1.1) enable one rule, then modify is made.
    // 1.2) disable the rule, and enable the next.
    // 1.3) disable rule to and enable rule 3.
    @Test
    public  void testScenario_1(){
        KieSession kSession = getSessionFromString( this.drl );

        BankAccount baAbc = new BankAccount("123456789","Account Abc",0);
        FactHandle fh_baAbc = kSession.insert(baAbc);
        Request abc = new Request(baAbc);
        kSession.insert(abc);

        System.out.println("\nTest Scenario 1: enable rule1, disable rule1, enable rule2, disable rule2, enable rule3.");

        // 1.1) enable rule1
        FactHandle fh_rule1 = kSession.insert("rule1");
        kSession.fireAllRules();
        System.out.println("rule1 enabled - Bank account summary: " + baAbc.accountSummary());
        try{
            Assert.assertEquals ("Balance after enabling rule1 is not 100, as expected.\n",100f, baAbc.getBalance(),0.0);
        }catch (AssertionError e){
            System.out.println(e.getMessage());
        }
        // 1.2) disable rule1, enable rule2
        kSession.delete(fh_rule1);
        FactHandle fh_rule2 = kSession.insert("rule2");
        kSession.fireAllRules();
        System.out.println("rule1 disabled, rule2 enabled - Bank account summary: " + baAbc.accountSummary());
        try{
            Assert.assertEquals("Balance after disabling rule1 & enabling rule2 is not 99 as expected.\n",99f, (float)baAbc.getBalance(), 0.0);
        }catch (AssertionError e){
            System.out.println(e.getMessage());
        }
        // 1.3) disable rule2, enable rule3
        kSession.delete(fh_rule2);
        FactHandle fh_rule3 = kSession.insert("rule3");
        kSession.fireAllRules();
        System.out.println("rule2 disabled, rule2 enabled - Bank account summary: " + baAbc.accountSummary());
        try {
            Assert.assertEquals("Balance after disabling rule2 & enabling rule3 is not 50 as expected.\n",50f, (float) baAbc.getBalance(), 0.0);
        }catch (AssertionError e){
            System.out.println(e.getMessage());
        }}

    //2.1) enable first rule 2.2) enable second rule 2.3) enable third rule. 2.4) disable first rule.
    //2.2 and 2.3 should result in no change. the quality is added to the set, but the prime does not change. 2.4 should result in a prime change, and the second rule's value is approved.
    @Test
    public void testScenario_2(){
        KieSession kSession = getSessionFromString( this.drl );

        BankAccount baAbc = new BankAccount("123456789","Account Abc",0);
        FactHandle fh_baAbc = kSession.insert(baAbc);
        Request abc = new Request(baAbc);
        kSession.insert(abc);

        System.out.println("\n\nTest Scenario 2: enable rule1, enable rule2, enable rule3, disable rule1.");

        // 2.1) enable rule1
        FactHandle fh_rule1 = kSession.insert("rule1");
        kSession.fireAllRules();
        System.out.println("rule1 enabled - Bank account summary: " + baAbc.accountSummary());
        try{
            Assert.assertEquals ("Balance after enabling rule1 is not 100, as expected.\n",100f, baAbc.getBalance(),0.0);
        }catch (AssertionError e){
            System.out.println(e.getMessage());
        }
        // 2.2) enable rule2
        FactHandle fh_rule2 = kSession.insert("rule2");
        kSession.fireAllRules();
        System.out.println("rule2 enabled - Bank account summary: " + baAbc.accountSummary());
        try{
            Assert.assertEquals ("Balance after enabling rule2 is not 100, as expected.\n",100f, baAbc.getBalance(),0.0);
        }catch (AssertionError e){
            System.out.println(e.getMessage());
        }
        // 2.3) enable rule3
        FactHandle fh_rule3 = kSession.insert("rule3");
        kSession.fireAllRules();
        System.out.println("rule3 enabled - Bank account summary: " + baAbc.accountSummary());
        try{
            Assert.assertEquals ("Balance after enabling rule3 is not 100, as expected.\n",100f, baAbc.getBalance(),0.0);
        }catch (AssertionError e){
            System.out.println(e.getMessage());
        }
        // 2.4) disable rule1
        kSession.delete(fh_rule1);
        kSession.fireAllRules();
        System.out.println("rule1 disabled - Bank account summary: " + baAbc.accountSummary());
        try{
            Assert.assertEquals ("Balance after disabling rule1 is not 99, as expected.\n",99f, baAbc.getBalance(),0.0);
        }catch (AssertionError e){
            System.out.println(e.getMessage());
        }
    }

    // 3) is almost the same as 2. but this time enable rule1, then enable rule3 then rule2.
    // just trying to show it's not rule order in the drl file - but the order of activation that specifies the priority.
    @Test
    public void testScenario_3(){
       KieSession kSession = getSessionFromString(this.drl );

       BankAccount baAbc = new BankAccount("123456789","Account Abc",0);
       FactHandle fh_baAbc = kSession.insert(baAbc);
       Request abc = new Request(baAbc);
       kSession.insert(abc);

       System.out.println("\n\nTest Scenario 3: enable rule1, enable rule3, enable rule2, disable rule1.");

       // 2.1) enable rule1
       FactHandle fh_rule1 = kSession.insert("rule1");
       kSession.fireAllRules();
       System.out.println("rule1 enabled - Bank account summary: " + baAbc.accountSummary());
       try{
           Assert.assertEquals ("Balance after enabling rule1 is not 100, as expected.\n",100f, baAbc.getBalance(),0.0);
       }catch (AssertionError e){
           System.out.println(e.getMessage());
       }
       // 2.2) enable rule3
       FactHandle fh_rule3 = kSession.insert("rule3");
       kSession.fireAllRules();
       System.out.println("rule3 enabled - Bank account summary: " + baAbc.accountSummary());
       try{
           Assert.assertEquals ("Balance after enabling rule3 is not 100, as expected.\n",100f, baAbc.getBalance(),0.0);
       }catch (AssertionError e){
           System.out.println(e.getMessage());
       }
       // 2.3) enable rule2
       FactHandle fh_rule2 = kSession.insert("rule2");
       kSession.fireAllRules();
       System.out.println("rule2 enabled - Bank account summary: " + baAbc.accountSummary());
       try{
           Assert.assertEquals ("Balance after enabling rule2 is not 100, as expected.\n",100f, baAbc.getBalance(),0.0);
       }catch (AssertionError e){
           System.out.println(e.getMessage());
       }
       // 2.4) disable rule1
       kSession.delete(fh_rule1);
       kSession.fireAllRules();
       System.out.println("rule1 disabled - Bank account summary: " + baAbc.accountSummary());
       try{
           Assert.assertEquals ("Balance after disabling rule1 is not 50, as expected.\n",50f, baAbc.getBalance(),0.0);
       }catch (AssertionError e){
           System.out.println(e.getMessage());
       }
    }

    public static class Request{
        private BankAccount ba;

        public Request(BankAccount ba) {
            this.ba = ba;
        }

        public BankAccount getBa() {
            return ba;
        }
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

        KieSession session = kbase.newKieSession();
        return session;
    }

}
