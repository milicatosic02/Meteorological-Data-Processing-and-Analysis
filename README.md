# Meteorological Data Processing and Analysis  

## Project Overview  
This project implements a **multithreaded system** for processing large meteorological data files (`.txt` and `.csv`). The system monitors a target directory for changes, processes files containing station names and recorded temperatures, and enables users to run additional tasks and check their status via a **command-line interface (CLI)**. Special attention was given to handling very large files efficiently (up to 14GB) without loading them entirely into memory.  

## Key Features  
- **Directory Monitoring** – Detects newly added or modified `.txt` and `.csv` files in real time.  
- **Concurrent File Processing** – Uses `ExecutorService` with multiple threads to parse files line by line and update an in-memory map with aggregated statistics (station counts and temperature sums per starting letter).  
- **Synchronization** – Ensures safe concurrent access to shared data structures.  
- **Command-Line Interface (CLI)** – Supports flexible commands with long and short options, including:  
  - `SCAN` – Search stations within a temperature range, filtered by initial letter, and export results to a file.  
  - `STATUS` – Check the execution status of a given job.  
  - `MAP` – Display aggregated in-memory statistics.  
  - `EXPORTMAP` – Export in-memory statistics to a CSV log.  
  - `START` / `STOP` – Start or gracefully shut down the system, with support for saving or loading unfinished jobs.  
- **Periodic Reporting** – Generates automatic CSV reports of the in-memory map every minute.  
- **Error Handling** – Provides clear, user-friendly error messages without stack traces, ensuring continuous system operation.  

## Technical Highlights  
- Efficient file reading using streams (`BufferedReader`) to prevent memory overload.  
- Support for CSV headers and guaranteed correct file format assumptions.  
- Robust job management with unique identifiers, blocking queues, and thread-safe execution.  
- Graceful shutdown of all threads and services with optional saving of pending jobs.  

## Learning Outcomes  
Through this project, I gained practical experience in:  
- Designing and implementing **multithreaded systems** in Java.  
- Working with **ExecutorService, synchronization, and thread safety**.  
- Building a **command-line interface** with flexible argument parsing.  
- Managing **large-scale data processing** efficiently.  
- Ensuring reliability and fault tolerance in concurrent applications.  

---
🚀 Developed as part of the **Concurrent and Distributed Systems** course (2025).  
