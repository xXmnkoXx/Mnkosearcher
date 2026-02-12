package com.ordenatec.portallicitaciones.infra.importacion.plcsp;

public class ImportJobStatus {

    public enum State { RUNNING, DONE, ERROR }

    private volatile int total;
    private volatile int processed;
    private volatile int inserted;
    private volatile int updated;
    private volatile int skipped;
    private volatile int errors;
    private volatile State state = State.RUNNING;

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getProcessed() { return processed; }
    public void incProcessed() { this.processed++; }

    public int getInserted() { return inserted; }
    public void incInserted() { this.inserted++; }

    public int getUpdated() { return updated; }
    public void incUpdated() { this.updated++; }

    public int getSkipped() { return skipped; }
    public void incSkipped() { this.skipped++; }

    public int getErrors() { return errors; }
    public void incErrors() { this.errors++; }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
}
