package command;

public enum JobStatus {
    PENDING,    // Posao je u redu za izvršavanje, ali još nije započet
    RUNNING,    // Posao se trenutno izvršava
    COMPLETED,  // Posao je uspešno završen
    FAILED      // Došlo je do greške tokom izvršavanja
}