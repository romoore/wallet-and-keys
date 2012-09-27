/*
 * Owl Platform
 * Copyright (C) 2012 Robert Moore and the Owl Platform
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.owlplatform.solver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import com.owlplatform.worldmodel.client.ClientWorldConnection;
import com.owlplatform.worldmodel.client.StepResponse;
import com.owlplatform.worldmodel.solver.SolverWorldConnection;
import com.thoughtworks.xstream.XStream;

/**
 * A simple solver that tries to detect whether you've forgotten something on
 * your way out based on the mobility of your things and a door switch. If any
 * of the items listed as part of a group have not moved together within some
 * number of seconds before a door opens, then it produces an alert.
 * 
 * @author Robert Moore
 * 
 */
public class WalletAndKeys extends Thread{

  /**
   * Parses the arguments and gets things started.
   * @param args <Config File>
   * @throws Throwable 
   */
  public static void main(String[]args) throws Throwable{
    if(args.length < 1){
      System.err.println("Missing one or more arguments:\n\t<Config File>");
      return;
    }
    
    XStream configReader = new XStream();
    WAKConfig conf = (WAKConfig)configReader.fromXML(new File(args[0]));
    System.out.println(configReader.toXML(conf));
   
    final WalletAndKeys app = new WalletAndKeys(conf);
    
    Runtime.getRuntime().addShutdownHook(new Thread(){
      public void run(){
        app.shutdown();
      }
    });
    
  }
  
  private final WAKConfig config;
  private boolean keepRunning = true;
  
  private final ClientWorldConnection asClient = new ClientWorldConnection();
  private final SolverWorldConnection asSolver = new SolverWorldConnection();
  
  public WalletAndKeys(final WAKConfig config){
    super("Main Thread");
    this.config = config;
    
    this.asClient.setHost(this.config.getWorldModelHost());
    this.asClient.setPort(this.config.getWorldModelClientPort());
    
    this.asSolver.setHost(this.config.getWorldModelHost());
    this.asSolver.setPort(this.config.getWorldModelSolverPort());
  }
  
  public void run(){
    StringBuilder itemRegexBuilder = new StringBuilder("^(");
    
    for(int i = 0; i < this.config.getRequiredItems().length; ++i){
      if(i > 0){
        itemRegexBuilder.append("|");
      }
      itemRegexBuilder.append(this.config.getRequiredItems()[i]);
    }
    itemRegexBuilder.append(")$");
    while(this.keepRunning){
      
      StepResponse subscription = this.asClient.getStreamRequest(itemRegexBuilder.toString(), System.currentTimeMillis(), 0, "mobility");
    }
    
    this.asClient.disconnect();
    this.asSolver.disconnect();
  }
  
  public void shutdown(){
    this.keepRunning = false;
  }
  
}
