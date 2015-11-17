# JavaPM
Allows processes to be migrated within the JVM.
==============================================
 Extended JavaPM sources to migrate a GUI notepad process over java_1.8.60u 
-----------------------------------------------

* Specifics of implementation *
The project provides framework to migrate an instance of any class that extends the MigratableProcess class to annother JVM running the same program. 
Added a class SampleGuiProcess that extends MigratableProcess (so that it can be migrated). The class uses JFrame to create a gui and implements ActionListener to handle user responses.

- Server port :

* Instructions *
- Run JavaPM.jar from the directory .
- mvn exec:java -Dexec:mainClass=javapm.process.ProcessManager -Dexec.args="172.31.76.69"

