package org.drools.mvel.compiler.beliefsystem.chainbs;

public class Person {
    private String name;
    private String hairColor;

    public Person(String name){
        this.name = name;
        this.hairColor = "unknown"; //default value
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return this.name;
    }

    public void setHairColor(String hairColor){
        this.hairColor = hairColor;
    }

    public String getHairColor(){
        return this.hairColor;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + this.name + '\'' +
                ", hairColor='" + this.hairColor + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        Person person = (Person) o;

        if (!name.equals(person.name)) { return false; }
        if (!hairColor.equals(person.hairColor)) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + hairColor.hashCode();
        return result;
    }
}
