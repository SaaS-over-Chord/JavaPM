/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javapm.process;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Scanner;
import javapm.io.TransactionalFileInputStream;
import javapm.io.TransactionalFileOutputStream;
import javax.swing.*;

/**
 * The sample Gui process example of <code>MigratableProcess</code>
 * It is a proof of concept that a GIU application can be migrated using this framework
 * @author Pratyush Kumar(pratyush)
 * @author Vasu Vardhan(vardhan)
 * @see javapm.process.MigratableProcess
 * @see javapm.io.TransactionalFileInputStream
 * @see javapm.io.TransactionalFileOutputStream
 */
public class SampleGuiProcess extends MigratableProcess implements ActionListener{

    JFrame appFrame = new JFrame(); 
    private TextArea textArea;
    private MenuBar menuBar; 
    private Menu file; 
    private MenuItem openFile;  
    private MenuItem saveFile; 
    private MenuItem close; 


    public SampleGuiProcess(String args[]) throws Exception
    {
        super(args);
        this.textArea = new TextArea("", 0,0, TextArea.SCROLLBARS_VERTICAL_ONLY);
        this.menuBar = new MenuBar();
        this.file = new Menu();
        this.close = new MenuItem();
        this.saveFile = new MenuItem();
        this.openFile = new MenuItem();
        appFrame.setSize(500, 300); // set the initial size of the window
        appFrame.setTitle("Java Notepad Tutorial"); // set the title of the window
        appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // set the default close operation (exit when it gets closed)
        textArea.setFont(new Font("Century Gothic", Font.BOLD, 12)); // set a default font for the TextArea
        // this is why we didn't have to worry about the size of the TextArea!
        appFrame.getContentPane().setLayout(new BorderLayout()); // the BorderLayout bit makes it fill it automatically
        appFrame.getContentPane().add(textArea);
        appFrame.setAlwaysOnTop(false);
        // add our menu bar into the GUI
        appFrame.setMenuBar(this.menuBar);
        this.menuBar.add(this.file); // we'll configure this later

        // first off, the design of the menuBar itself. Pretty simple, all we need to do
        // is add a couple of menus, which will be populated later on
        this.file.setLabel("File");

        // now it's time to work with the menu. I'm only going to add a basic File menu
        // but you could add more!

        // now we can start working on the content of the menu~ this gets a little repetitive,
        // so please bare with me!

        // time for the repetitive stuff. let's add the "Open" option
        this.openFile.setLabel("Open"); // set the label of the menu item
        this.openFile.addActionListener(this); // add an action listener (so we know when it's been clicked
        this.openFile.setShortcut(new MenuShortcut(KeyEvent.VK_O, false)); // set a keyboard shortcut
        this.file.add(this.openFile); // add it to the "File" menu

        // and the save...
        this.saveFile.setLabel("Save");
        this.saveFile.addActionListener(this);
        this.saveFile.setShortcut(new MenuShortcut(KeyEvent.VK_S, false));
        this.file.add(this.saveFile);

        // and finally, the close option
        this.close.setLabel("Close");
        // along with our "CTRL+F4" shortcut to close the window, we also have
        // the default closer, as stated at the beginning of this tutorial.
        // this means that we actually have TWO shortcuts to close:
        // 1) the default close operation (example, Alt+F4 on Windows)
        // 2) CTRL+F4, which we are about to define now: (this one will appear in the label)
        this.close.setShortcut(new MenuShortcut(KeyEvent.VK_F4, false));
        this.close.addActionListener(this);
        this.file.add(this.close);
        
    }
	
    @Override
    public void actionPerformed (ActionEvent e) {
        // if the source of the event was our "close" option
        if (e.getSource() == this.close)
        {
            appFrame.dispose();
        } // dispose all resources and close the application
    
        // if the source was the "open" option
        else if (e.getSource() == this.openFile) {
            JFileChooser open = new JFileChooser(); // open up a file chooser (a dialog for the user to browse files to open)
            int option = open.showOpenDialog(appFrame); // get the option that the user selected (approve or cancel)
            // NOTE: because we are OPENing a file, we call showOpenDialog~
            // if the user clicked OK, we have "APPROVE_OPTION"
            // so we want to open the file
        if (option == JFileChooser.APPROVE_OPTION) {
            this.textArea.setText(""); // clear the TextArea before applying the file contents
            try {
            // create a scanner to read the file (getSelectedFile().getPath() will get the path to the file)
                Scanner scan = new Scanner(new FileReader(open.getSelectedFile().getPath()));
                while (scan.hasNext()) // while there's still something to read
                this.textArea.append(scan.nextLine() + "\n"); // append the line to the TextArea
            }
            catch (Exception ex) { // catch any exceptions, and...
            // ...write to the debug console
            System.out.println(ex.getMessage());
            }
        }
    }
    
    // and lastly, if the source of the event was the "save" option
    else if (e.getSource() == this.saveFile) {
      JFileChooser save = new JFileChooser(); // again, open a file chooser
      int option = save.showSaveDialog(appFrame); // similar to the open file, only this time we call
      // showSaveDialog instead of showOpenDialog
      // if the user clicked OK (and not cancel)
      if (option == JFileChooser.APPROVE_OPTION) {
        try {
          // create a buffered writer to write to a file
          BufferedWriter out = new BufferedWriter(new FileWriter(save.getSelectedFile().getPath()));
          out.write(this.textArea.getText()); // write the contents of the TextArea to the file
          out.close(); // close the file stream
        } catch (Exception ex) { // again, catch any exceptions and...
          // ...write to the debug console
          System.out.println(ex.getMessage());
        }
      }
    }
}
    
    /**
     * Implementation of <code>processing()</code> from
     * <code>MigratableProcess</code>.
     * This function should loop with the <code>suspending</code>
     * and <code>dead</code> flag.
     * First read a line as string, Second convert it to character array,
     * third sort them, fourth convert back to string, finally write the
     * sort result to a file. There are stops(200ms) between each step.
     * The process can resume to a perticular step after migration.
     * 
     * @throws IOException
     */
    @Override
    public void processing() throws IOException {
    	//findStreamField();
        appFrame.setVisible(true);
        while(!suspending){
            
        }
        appFrame.setVisible(false);
    }
}
