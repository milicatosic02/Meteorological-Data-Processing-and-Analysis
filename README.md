Domaci na srpskom:

Obrada i analiza meteoroloških podataka
Zadatak je implementirati sistem koji se sastoji od više konkurentnih niti za obradu velikih tekstualnih datoteka (sa ekstenzijom .txt i .csv) u zadatom direktorijumu. Sistem treba da prati promene u tom direktorijumu, obradi fajlove koji sadrže podatke o meteorološkim stanicama i zabeleženim temperaturama, i da omogućava korisniku da putem komandne linije pokreće dodatne zadatke i proverava status izvršenja poslova.
Podaci u fajlovima su organizovani po sledećem formatu (svaka linija predstavlja jedno merenje):
Hamburg;12.0
Bulawayo;8.9
Palembang;38.8
St. John's;15.2
Cracow;12.6
Bridgetown;26.9
Istanbul;6.2
Roseau;34.4
Conakry;31.2
Istanbul;23.0
1 Komponente sistema
1.1 Nit za monitoring direktorijuma
Osluškuje zadati direktorijum (definisan putem komandne linije ili konfiguracionog fajla) i detektuje nove ili izmenjene fajlove sa ekstenzijom .txt i .csv.
Za svaki fajl se beleži “last modified” vrednost – ukoliko je fajl već obrađen (prethodna i trenutna vrednost su iste), novi posao se ne pokreće.
Čitanje fajlova vrši se deo po deo (ne smeštati čitav fajl u memoriju) zbog potencijalno ogromne veličine fajlova (measurements_big.txt je ~ 14GB). 
Kada se detektuje promena u direkotrijumu potrebno je ispisati poruku koji fajlovi su se izmenili i/ili dodali.

1.2 Obrada fajlova i ažuriranje in-memory mape
Kada se otkrije promena (i kada se prvi putpokrene) pokreće se posao (preko ExecutorService) koji obrađuje sve fajlove unutar direktorijuma. Svaka nit unutar ExecutorService-a treba da radi na jednom fajlu. Preporuka je koristiti 4 niti unutar servisa.
Svaka linija u fajlu sadrži naziv meteorološke stanice i zabeleženu temperaturu. Podaci se kombinuju u in-memory mapi organizovanoj abecedno, gde se za svako slovo (prvo slovo naziva stanice) čuva: broj stanica koje počinju tim slovom i suma svih merenja za te stanice.
Ovaj zadatak obrade fajlova koji ažurira mapu mora biti zaštićen mehanizmima sinhronizacije kako se ne bi sudarila sa drugim operacijama čitanja istih fajlova.
Napomena: U slučaju obrade CSV fajla potrebno je preskočiti zaglavlje.
Napomena: Smatra se da će sadržaj svih fajlova unutar direktorijuma biti u korektnom formatu.

1.3 CLI nit i obrada komandi
Korisnik unosi komande putem komandne linije. Sve komande se upisuju u blokirajući red (STOP i START ne), a posebna nit periodično čita iz tog reda i delegira zadatke.
Komande imaju argumente koje se se mogu zadati u bilo kom redosledu i moraju biti označene prefiksom “--” (dugi oblik) ili jednostavnom crticom “-” (kratki oblik). Komanda se može napisati kao:
SCAN --min 10.0 --max 20.0 --letter H --output output.txt --job job1
ili kratko:
SCAN -m 10.0 -M 20.0 -l H -o output.txt -j job1
Sistem mora validirati sve primljene komande te, ukoliko je neka komanda neispravna ili nedostaju potrebni argumenti, ispisati jasnu grešku (bez stack trace-a) i nastaviti rad.
CLI nit ni u jednom trenutku ne sme biti blokirana.

1.3.1 Komanda SCAN
Pretražuje sve fajlove u nadgledanom direktorijumu i pronalazi meteorološke stanice čiji naziv počinje zadatim slovom i za koje je temperatura u opsegu [min, max]. Svaki fajl treba da se obradi sa jednom niti unutar ExecutorService-a, gde se oni kasnije upisuju u izlazni fajl (output) čije se ime zadaje kao argument komande SCAN. 
Voditi računa da se rezultati pronalaženja stanica za fajlove ne čuvaju i ne kombinuju u memoriji jer se radi sa velikim fajlovima (Java OutOfMemoryError).
Argumenti:
--min (ili -m): minimalna temperatura
--max (ili -M): maksimalna temperatura
--letter (ili -l): početno slovo meteorološke stanice
--output (ili -o): naziv izlaznog fajla
--job (ili -j): naziv zadatka
Primer:
SCAN --min 10.0 --max 20.0 --letter H --output output.txt --job job1
Output fajl linija:
Hamburg;12.0

1.3.2 Komanda STATUS
Prikazuje trenutni status zadatka (pending, running ili completed) sa navedenim imenom.
Argumenti:
--job (ili -j): naziv zadatka
Primer:
STATUS --job job1
job1 is running
1.3.3 Komanda MAP
Ispisuje sadržaj in-memory mape – u 13 linija, gde svaka linija prikazuje po dva slova sa pripadajućim brojem stanica i sumom merenja. Voditi računa o situaciji kada je mapa nedostupna, u trenutnku kada se prvi put upisuju vrednosti, tada je potrebno ispisati poruku da mapa još uvek nije dostupna.
Primer:
MAP
a: 8524 - 1823412 | b: 5234 - 523512
c: 8523 - 5521342 | d: 1253 - 502395 …  

1.3.4 Komanda EXPORTMAP
Eksportuje sadržaj in-memory mape u log CSV fajl. CSV fajl sadrži kolone: "Letter", "Station count", "Sum".  Svaki red log fajla, koji se čuva u okviru projekta na proizvoljnoj lokaciji, treba da sadrži podatke u formatu:
a 8524 1823412
b 5234 523512 …
Primer:
EXPORTMAP

1.3.4 Komanda SHUTDOWN
Na elegantan način zaustavlja ceo sistem – prekida sve ExecutorService-ove i signalizira svim nitima da uredno završe rad. Pored toga, ako se doda opcija:
--save-jobs (ili -s), svi neizvršeni poslovi se sačuvaju u poseban load_config fajl. Neizvršene poslove možete čuvati u bilo kom formatu.
Primer:
SHUTDOWN --save-jobs

1.3.5 Komanda START
Pokreće sistem. Dodatna opcija --load-jobs (ili -l) omogućava da se, ako postoji fajl (load_config) sa sačuvanim zadacima, ti poslovi učitaju i automatski dodaju u red za izvršavanje. Ukoliko posao ne može biti započet, jer na primer neko drugi radi sa fajlom, nije dozvoljeno odbaciti taj posao.
Primer:
START --load-jobs

1.4 Periodični izveštaj
Potrebno je implementirati dodatnu nit koja će, svakog minuta, generisati automatski izveštaj o trenutnom stanju in-memory mape i upisivati ga u log CSV fajl (isti fajl koji se koristi za EXPORTMAP) komandu. Vodite računa da se izveštaj ne meša, odnosno ne izvršava istovremeno, sa ručnim logovanjem (preko komande EXPORTMAP).

1.5 Izvršavanje zadataka preko ExecutorService
Poslove koji se odnose na obradu fajlova (npr. čitanje fajlova, pretraga podataka) podelite na manje zadatke koji se paralelno izvršavaju. Preporučeno je korišćenje fork/join pool-a ili klasičnog thread pool-a (minimum 4 niti).
Svaki zadatak mora imati jedinstveni identifikator (naziv zadatka) radi praćenja statusa.


2 Tehnički zahtevi i smernice
Fajlovi se čitaju deo po deo (korišćenjem streamova ili BufferedReadera) kako se ne bi preopteretila memorija.
Koristite odgovarajuće mehanizme da osigurate da se operacije nad fajlovima i ažuriranje mape ne izvršavaju istovremeno sa operacijama pretrage i izvoza rezultata.
Sistem NE sme da pukne ni u jednom trenutku.
Svi izuzeci se moraju obraditi kulturno – ispisati korisniku kratke, jasne poruke (npr. "Greška pri čitanju fajla 'naziv_fajla'. Nastavljam rad."), bez prikazivanja kompletnog stack trace-a.
Argumenti za komande mogu biti zadati u bilo kojem redosledu, a prepoznaju se pomoću prefiksa (npr. --min ili -m, --output ili -o, itd.). Sistem mora validirati argumente i u slučaju greške obavestiti korisnika bez rušenja sistema.
Komanda SHUTDOWN mora da prekine sve niti na uredan način. Ukoliko se doda opcija --save-jobs, svi neizvršeni poslovi se sačuvaju u posebnom fajlu.
Preporuka je da se svi poslovi upisuju u centralni blokirajući red. Komponente kao što su nit koja detektuje izmene u direktorijumu i nit koja prihvata komande sa komandne linije dodaju poslove, dok posebna, druga nit preuzima poslove i delegira ih dalje.


Task: Processing and Analysis of Meteorological Data

The goal is to implement a multithreaded system that processes large text files (with .txt and .csv extensions) in a specified directory. The system should monitor changes in that directory, process files containing data about weather stations and recorded temperatures, and allow users to execute additional tasks and check job statuses via the command line.

The data in the files follows the format: each line represents a measurement with the name of the station and its temperature, separated by a semicolon.

System Components

Directory Monitoring Thread

This thread listens to the specified directory (defined via the command line or configuration file) and detects new or modified .txt and .csv files. For each file, the “last modified” timestamp is recorded — if a file has already been processed (previous and current timestamps are the same), the task will not be re-executed. Files must be read in parts (not loaded entirely into memory) due to their potentially large size. When a change is detected, a message should be printed indicating which files were added or modified.

File Processing and In-Memory Map Updating

Upon detecting a change (or during the initial run), a task is triggered (via ExecutorService) to process all files in the directory. Each thread within the ExecutorService should handle one file. It is recommended to use 4 threads. Each line contains a station name and temperature. Data is aggregated into an in-memory map sorted alphabetically. For each starting letter, the map stores the number of stations and the total sum of all their measurements. This process must be synchronized to prevent data collisions with other file reading operations. For CSV files, the header should be skipped. It is assumed all file content is in a valid format.

CLI Thread and Command Handling

The user interacts with the system via the command line. All commands are written into a blocking queue (except for STOP and START), and a dedicated thread periodically reads and delegates tasks from that queue. Commands can have arguments in any order, marked with a long form (--) or a short form (-). The system must validate all commands, and if a command is invalid or missing arguments, it should print a clear error message (without a stack trace) and continue running. The CLI thread must never be blocked.

3.1 SCAN Command

Searches all files in the monitored directory and finds weather stations starting with a specific letter and whose temperatures fall within a given range. Each file is processed by one thread within the ExecutorService, and matching lines are written to an output file. Results must not be held or aggregated in memory due to file size limitations.

3.2 STATUS Command

Displays the current status of a task (pending, running, or completed) based on the provided job name.

3.3 MAP Command

Prints the contents of the in-memory map — in 13 lines, each showing two letters with their respective station counts and measurement sums. If the map is not yet available (e.g., during the first update), a message should indicate that it is still unavailable.

3.4 EXPORTMAP Command

Exports the in-memory map to a log CSV file. The CSV contains the columns: "Letter", "Station count", and "Sum". Each line logs the values per letter.

3.5 SHUTDOWN Command

Gracefully shuts down the system — terminates all ExecutorService instances and signals all threads to finish their work properly. With the optional flag --save-jobs, all unexecuted jobs are saved to a configuration file in any format.

3.6 START Command

Starts the system. With the optional flag --load-jobs, previously saved jobs (from a configuration file) are loaded and added to the execution queue. If a job cannot be started (e.g., a file is already being used), it must not be discarded.

Periodic Report

An additional thread must generate a report every minute, showing the current state of the in-memory map and writing it to the same CSV file used for the EXPORTMAP command. Care must be taken to avoid conflicts between the periodic report and manual export.

Task Execution via ExecutorService

File processing tasks (e.g., reading, data searching) should be split into smaller tasks and executed in parallel. Using a fork/join pool or a fixed thread pool (minimum 4 threads) is recommended. Each task must have a unique identifier for status tracking.

Technical Requirements and Guidelines

Files must be read in chunks (using streams or BufferedReader) to avoid memory overload. Proper mechanisms must be used to ensure that file operations and map updates do not occur simultaneously with data searches or exports. The system must remain stable and never crash. All exceptions must be handled gracefully — with short and clear user messages (e.g., “Error reading file 'filename'. Continuing execution.”), without full stack traces. Command arguments can be in any order and are recognized by their prefixes. The system must validate input and notify the user of errors without crashing. The SHUTDOWN command must terminate all threads properly. If the --save-jobs option is provided, all pending jobs must be stored in a separate file.

It is recommended to use a central blocking queue for all tasks. Components such as the directory watcher thread and the CLI input thread should add jobs to the queue, while a separate worker thread retrieves and delegates them.





