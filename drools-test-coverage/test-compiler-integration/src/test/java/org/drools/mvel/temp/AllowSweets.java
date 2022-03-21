//package org.drools.tms.temp;
package org.drools.mvel.temp;

public class AllowSweets {
    private Person person;

    public AllowSweets(Person person) {
        this.person = person;
    }

    public Person getBird() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        AllowSweets allowsweets = (AllowSweets) o;

        if (!person.equals(allowsweets.person)) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        return person.hashCode();
    }
}
