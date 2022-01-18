package org.drools.mvel.temp;

public class DenySweets {
    private Person person;

    public DenySweets (Person person) {
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

        DenySweets denysweets = (DenySweets) o;

        if (!person.equals(denysweets.person)) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        return person.hashCode();
    }
}
