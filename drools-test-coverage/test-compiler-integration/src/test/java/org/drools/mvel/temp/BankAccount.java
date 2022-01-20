package org.drools.mvel.temp;

public class BankAccount {
    private String accountNo;
    private String accountName;
    private float balance;

    public BankAccount(String accNo, String accName, float initBalance) {
        this.accountNo = accNo;
        this.accountName = accName;
        this.balance = initBalance;
    }

    public String accountSummary(){
        return "accountNo=" + this.accountNo + ", accountName=" + this.accountName + ", Balance=" + this.balance;
    }

    public void debit(float amount) {
        this.balance -= amount;
    }

    public void credit(float amount) {
        this.balance += amount;
    }

    // set/get fields
    // set:
    public void setAccountNo(String accNo) {
        this.accountNo = accNo;
    }
    public void setAccountName(String accName) {
        this.accountName = accName;
    }
    public void setBalance(float balance) {
        this.balance = balance;
    }
    // get:
    public String getAccountNo() {
        return this.accountNo;
    }
    public String getAccountName() {
        return this.accountName;
    }
    public float getBalance() {
        return this.balance;
    }

    public int hashCode() {
        int result = this.accountNo.hashCode();
        result = 31 * result + this.accountName.hashCode();
        return result;
    }

    public boolean equals(Object o) {

        if (!(o instanceof BankAccount)) {
            return false;
        }

        BankAccount ba = (BankAccount)o;
        if (ba.accountName != this.accountName) {
            return false;
        }else if (ba.accountNo != this.accountNo){
            return false;
        }else {return true;}
    }

}
