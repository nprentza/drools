package org.drools.mvel.compiler.beliefsystem.chainbs;

import org.drools.core.BeliefSystemType;
import org.drools.core.ClassObjectFilter;
import org.drools.core.SessionConfiguration;
import org.drools.core.impl.RuleBaseFactory;
import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieBaseUtil;
import org.drools.testcoverage.common.util.TestParametersUtil;
import org.drools.tms.beliefsystem.chainbs.UpdateRestoreCommand;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.rule.FactHandle;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ChainSimpleJtmsTest {

    private final KieBaseTestConfiguration kieBaseTestConfiguration;

    private String drl1_test = "package org.drools.tms.temp; \n " +
            "import " + Person.class.getCanonicalName() + "; \n"  +
            "import " + UpdateRestoreCommand.class.getCanonicalName() + "; \n"  +
            "import " + Map.class.getCanonicalName() + "; \n" +
            "import " + HashMap.class.getCanonicalName() + "; \n" +
            "import " + Request.class.getCanonicalName() + "; \n" +
            " " +
            "rule Rule1 when \n" +
            "   String( this == 'rule1') \n" +
            "   r : Request() \n" +
            "then \n" +
            "    Map<String,Object> m = new HashMap<>(); \n " +
            "    m.put(\"hairColor\",\"blue\"); \n " +
            "    insertLogical( new UpdateRestoreCommand(kcontext.getKieRuntime().getFactHandle(r.getPerson()), m) ); \n " +
            "end \n" +
            "rule Rule2 when \n" +
            "   String( this == 'rule2') \n" +
            "   r : Request() \n" +
            "then \n" +
            "    Map<String,Object> m = new HashMap<>(); \n " +
            "    m.put(\"hairColor\",\"red\"); \n " +
            "    insertLogical( new UpdateRestoreCommand(kcontext.getKieRuntime().getFactHandle(r.getPerson()), m) ); \n " +
            "end \n" ;

    public ChainSimpleJtmsTest(final KieBaseTestConfiguration kieBaseTestConfiguration) {
        this.kieBaseTestConfiguration = kieBaseTestConfiguration;
    }

    @Parameterized.Parameters(name = "KieBase type={0}")
    public static Collection<Object[]> getParameters() {
        return TestParametersUtil.getKieBaseCloudConfigurations(false, true);
    }

    protected KieSession getSessionFromString(String drlString, BeliefSystemType bsType) {
        KieBase kBase;

        try {
            System.setProperty("drools.negatable", "on");
            kBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("test", kieBaseTestConfiguration, drlString);
        } finally {
            System.setProperty("drools.negatable", "off");
        }

        KieSessionConfiguration ksConf = RuleBaseFactory.newKnowledgeSessionConfiguration();
        ((SessionConfiguration) ksConf).setBeliefSystemType( bsType ); //BeliefSystemType.JTMS

        KieSession kSession = kBase.newKieSession( ksConf, null );
        return kSession;
    }

    @Test
    public void testDrools6921() {
        KieSession session = getSessionFromString( this.drl1_test , BeliefSystemType.SIMPLE);

        Person mary = new Person("Mary");
        FactHandle fh_mary = session.insert(mary);
        Request abc = new Request(mary);
        session.insert(abc);
        FactHandle handle1 = session.insert("rule1");

        session.fireAllRules();

        Collection cObjects = session.getObjects( new ClassObjectFilter( Person.class ) );
        Person p =  (Person) cObjects.toArray()[0];
        assertThat(p.getHairColor().equals("blue")).isTrue();
        System.out.println(p.toString());

        FactHandle handle2 = session.insert("rule2");
        session.fireAllRules();
        cObjects = session.getObjects( new ClassObjectFilter( Person.class ) );
        p =  (Person) cObjects.toArray()[0];
        assertThat(p.getHairColor().equals("red")).isTrue();
        System.out.println(p.toString());

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

