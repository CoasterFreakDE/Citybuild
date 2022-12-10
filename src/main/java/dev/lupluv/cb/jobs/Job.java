package dev.lupluv.cb.jobs;

public class Job {

    public static Job fromJobType(JobType jobType){
        return null;
    }

    public static Job fromJobName(String name){
        return null;
    }

    private String name;
    private double salary;
    private double multiplier;
    private double booster;

    private double level;
    private double experience;

    public Job(){}

    public Job(String name, double salary, double multiplier, double booster, double level, double experience) {
        this.name = name;
        this.salary = salary;
        this.multiplier = multiplier;
        this.booster = booster;
        this.level = level;
        this.experience = experience;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getBooster() {
        return booster;
    }

    public void setBooster(double booster) {
        this.booster = booster;
    }

    public double getLevel() {
        return level;
    }

    public void setLevel(double level) {
        this.level = level;
    }

    public double getExperience() {
        return experience;
    }

    public void setExperience(double experience) {
        this.experience = experience;
    }
}
