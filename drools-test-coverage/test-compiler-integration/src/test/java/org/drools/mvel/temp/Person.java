package org.drools.mvel.temp;

public class Person {
    private String name;
    private String onDiet;
    private String allowDenySweets; // "allow" or "deny"

    public Person(String name, String onDiet) {
        this.name = name;
        this.onDiet = onDiet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOnDiet() {
        return onDiet;
    }

    public void setOnDiet(String onDiet) {
        this.onDiet = onDiet;
    }

    public String getAllowSweets(){
        return this.allowDenySweets;
    }

    public void setAllowSweets(String allowOrDeny){
        this.allowDenySweets = allowOrDeny;
    }
    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", onDiet='" + onDiet + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Person person = (Person) o;

        if (!name.equals(person.name)) { return false; }
        if (!onDiet.equals(person.onDiet)) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + onDiet.hashCode();
        return result;
    }

}
