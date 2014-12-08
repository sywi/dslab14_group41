Reflect about your solution!

Summary:

Allgemein:

Das gesamte Projekt wurde von mir alleine verwirklicht.
Die Shell habe ich nicht verwendet. Der Grund hierfür ist, dass ich sie zuerst übersehen habe und später wäre es zu kompliziert gewesen sie doch noch einzubauen. 


Client:

Der Client an sich ist ein Thread, der in seiner Main-Methode gestartet wird. In der run()-Methode fragt er nun ständig ab, ob es neuen Input von der Konsole gibt. 
Wenn es sich um ein gültiges Kommando handelt, wird dieses an den Cloud Controller weiter gesendet. 

Wenn beim Start des Clients kein Cloud Controller zur Verfügung steht, beendet sich der Client automatisch wieder. Sollte die Verbindung beim Start erfolgreich 
aufgebaut worden sein, aber der Cloud Controller fällt kurzfristig aus, versucht der Client noch einmal sich zu verbinden und falls dies fehl schlägt beendet er sich.

Bei der Methode logout() wird die Verbindung zum Server aufrecht erhalten. Bei exit() werden alle Verbindungen getrennt, der Socket geschlossen und der Client beendet.



CloudController:

Im Cloud-Controller werden verschiedene Threads gestartet:

1. Client: Ein Thread hört auf den Socket und wartet auf Client Anfragen. Sobald ein Client auf diesem Socket erscheint, wird dieser Client in einem eigenen Thread 
(dem sogenannten WorkerThread) weiter behandelt.

	WorkerThread: 

	Der WorkerThread ist ein Thread, der Client Request bearbeitet. Zuerst werden Reader und Writer initiiert und auf 	User-Input gewartet. Die einzelnen Befehle 
	werden vom WorkerThread behandelt und falls nötig, an den CloudController oder an einen Node weitergeleitet. Welcher Node verwendet wird, entscheidet der 
	CloudController in der Methode getNode().


2. Node: Ein weiterer Thread wartet auf einkommende Pakete von den verschiedenen Nodes. Aus jedem einkommenden Paket wird der TCP Port und die Operatoren des jeweiligen 
Nodes gelesen. 

	Verwaltung von Nodes:

	Ankommende Pakete werden ausgelesen. TCP Port ist für jeden Node eindeutig, daher wird dieser als 	Schlüssel verwendet um die verschiedenen Nodes zu unterscheiden. 
	Kommt von einem Node das erste 	Paket an, wird der Node im Cloud Controller erzeugt. Dieser erhält einen TimeStamp, ein Boolean-Flag isAlive, den TCP Port, die 
	Adresse und die Operatoren, die der Node zur Verfügung stellt. Wurde von diesem Node schon einmal ein Paket empfangen (Identifizierung über TCP Port) dann wird 
	lediglich der TimeStamp auf die aktuelle Zeit gesetzt.

3. Node Überprüfung: Dieser Thread wartet die checkPeriod ab und geht dann die Liste an Nodes durch. Für jeden Node wird überprüft, wann das letzte Paket angekommen ist. 
Ist das zu lange her, wird der Node auf offline gesetzt. Das bedeutet, dass er nicht mehr für kommende Client-Anfragen zur Verfügung steht.

Diese drei Thread werden mit einem ThreadPool verwirklicht. Die WorkerThreads der Client-Anfragen, werden ebenfalls in einem ThreadPool ausgeführt. Auf diese Art und 
Weise, können sich beliebig viele Clients gleichzeitig einloggen. 





Node:

Im Node werden exakt 2 Threads gestartet.

1. IsAliveThread: Kümmert sich um das regelmäßige versenden des isAlive-Pakets.

2. Der zweite Thread hört auf das Socket, über welches die Client Anfragen hereinkommen. Wird auf dem Socket eine Anfrage empfangen, wird ein neuer Thread zur 
Bearbeitung dieser Anfrage erstellt (ClientRequestThread). Somit wird nicht der gesamte Node blockiert und er kann ohne Unterbrechung auf Clients warten.

	ClientRequestThread:
	
	Kümmert sich um einkommende Client Requests. Die Berechnung wird in diesem Thread ausgeführt. Auch das Loggen der Berechnungen übernimmt dieser Thread.
	
	
	
	
Probleme:

	Tests erfolgreich auf meinem eigenen Rechner, dem Lab-Rechner auf six.dslab.tuwien.ac.at, apollo.dslab.tuwien.ac.at
	
	Allerdings kam es auf starbuck.dslab.tuwien.ac.at zu einem Fehler.
	Grundsätzlich werden die isAlive-Packete mit der Nachricht 16500+- (TcpPort und Operatoren) gesendet. Die Nachricht, die vom Node wegging, war auch noch richtig, allerdings kam beim
	CloudController eine fehlerhafte Nachricht an: !alive 9872 +- 
	Da der TCP Port fehlt und diese Nachricht nicht wie erwartet aussieht konnte er CloudController diese nicht richtig verarbeiten. Es kam zu einer Exception und der CloudController 
	konnte die Nodes nicht in seine Liste aufnehmen.
	
	Problem Nr 2: Auf boomer.dslab.tuwien.ac.at kam es immer zu einer Address already in use! Exception. Da diese meines Wissens nach geworfen wird, wenn eine Ressource nicht 
	ordnungsgemäß geschlossen wird, habe ich nach diesem Problem gesucht, jedoch leider nicht gefunden. Es erschien mir allerdings auch etwas seltsam, da diese Fehlermeldung lediglich
	auf einem Server erschienen ist.
	
	
	