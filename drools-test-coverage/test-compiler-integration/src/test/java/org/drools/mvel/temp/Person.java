package org.drools.mvel.temp;

public class Person {
    private String name;
    private String onDiet;
    private String allowDenySweets; // "allow" or "deny"
    private String cheatDay;

    public Person(String name, String onDiet) {
        this.name = name;
        this.onDiet = onDiet;
        this.allowDenySweets = "tbd";
        this.cheatDay = "tbd";
    }

    public void setCheatDay(String day){
        this.cheatDay = day;
    }

    public String getCheatDay(){
        return this.cheatDay;
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

    public String getAllowDenySweets(){
        return this.allowDenySweets;
    }

    public void setAllowDenySweets(String allowOrDeny){
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
