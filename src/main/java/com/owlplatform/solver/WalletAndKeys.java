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
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.worldmodel.Attribute;
import com.owlplatform.worldmodel.client.ClientWorldConnection;
import com.owlplatform.worldmodel.client.StepResponse;
import com.owlplatform.worldmodel.client.WorldState;
import com.owlplatform.worldmodel.solver.SolverWorldConnection;
import com.owlplatform.worldmodel.solver.protocol.messages.AttributeAnnounceMessage.AttributeSpecification;
import com.owlplatform.worldmodel.types.BooleanConverter;
import com.owlplatform.worldmodel.types.DataConverter;
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
public class WalletAndKeys extends Thread {

  private static final Logger log = LoggerFactory
      .getLogger(WalletAndKeys.class);

  /**
   * Parses the arguments and gets things started.
   * 
   * @param args
   *          <Config File>
   * @throws Throwable
   */
  public static void main(String[] args) throws Throwable {
    if (args.length < 1) {
      System.err.println("Missing one or more arguments:\n\t<Config File>");
      return;
    }
    log.debug("Loading configuration file {}.", args[0]);
    XStream configReader = new XStream();
    WAKConfig conf = (WAKConfig) configReader.fromXML(new File(args[0]));
    log.debug("Configuration:\n{}", configReader.toXML(conf));
    final WalletAndKeys app = new WalletAndKeys(conf);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        log.info("Caught user shutdown request.");
        app.shutdown();
        log.debug("Shutdown invoked on solver.");
      }
    });
    log.debug("Created shutdown hook.");
    app.start();
    log.debug("Main thread exiting.");
  }

  private final WAKConfig config;
  private boolean keepRunning = true;
  private boolean doorOpen = false;

  private final ClientWorldConnection asClient = new ClientWorldConnection();
  private final SolverWorldConnection asSolver = new SolverWorldConnection();
  private final HashMap<String, MobilityState> itemMobility = new HashMap<String, MobilityState>();

  public WalletAndKeys(final WAKConfig config) {
    super("Main Thread");
    this.config = config;

    this.asClient.setHost(this.config.getWorldModelHost());
    this.asClient.setPort(this.config.getWorldModelClientPort());

    this.asSolver.setHost(this.config.getWorldModelHost());
    this.asSolver.setPort(this.config.getWorldModelSolverPort());
    this.asSolver.setOriginString(this.config.getOriginName());
    AttributeSpecification spec = new AttributeSpecification();
    spec.setAttributeName(this.config.getAlertAttribute());
    spec.setIsOnDemand(false);
    this.asSolver.addAttribute(spec);
  }

  public void run() {
    log.info("Starting wallet-and-keys solver.");
    String itemIds = buildEitherOrRegex(this.config.getRequiredItems());
    String doorIds = buildEitherOrRegex(this.config.getDoors());
    String mobilityAttributes = buildEitherOrRegex(this.config
        .getMobilityAttributeNames());
    String doorAttributes = buildEitherOrRegex(this.config
        .getOpenDoorAttributeNames());

    log.debug("Items: " + itemIds);
    log.debug("Doors: " + doorIds);
    log.debug("Mobility: " + mobilityAttributes);
    log.debug("Open: " + doorAttributes);

    

    for (String item : this.config.getRequiredItems()) {
      itemMobility.put(item, new MobilityState());
    }

    this.asClient.connect(10000);
    this.asSolver.connect(10000);

    while (this.keepRunning) {
      StepResponse mobilityResponse = this.asClient.getStreamRequest(itemIds,
          System.currentTimeMillis(), 0, mobilityAttributes);
      StepResponse doorResponse = this.asClient.getStreamRequest(doorIds,
          System.currentTimeMillis(), 0, doorAttributes);
      try {
        // Keep going until an error or
        while ((!mobilityResponse.isComplete() && !mobilityResponse.isError())
            && (!doorResponse.isComplete() && !doorResponse.isError())) {
          if (mobilityResponse.hasNext()) {
            log.debug("Received a mobility update");
            WorldState updateState = mobilityResponse.next();
            Collection<String> ids = updateState.getIdentifiers();
            for (String id : ids) {
              log.debug("Update for item {}", id);
              Collection<Attribute> attrs = updateState.getState(id);
              for (Attribute att : attrs) {
                Boolean newValue = (Boolean) DataConverter.decode(
                    att.getAttributeName(), att.getData());
                MobilityState currentState = itemMobility.get(id);
                currentState.setMobile(newValue.booleanValue());
                currentState.setLastMobile(System.currentTimeMillis());
                log.debug("{}: mobile? {}", id, newValue);
                // If we picked up the item while the door was open, reset the alert
                if(this.doorOpen && currentState.isMobile()){
                  this.resetAlert(id);
                }
              } // End Attributes
              
            } // End Identifiers
            
            
          } // End mobility response

          if (doorResponse.hasNext()) {
            
            Collection<String> missingItems = null;
            WorldState updateState = doorResponse.next();
            Collection<String> ids = updateState.getIdentifiers();
            for (String id : ids) {
              log.debug("Update for door {}", id);
              Collection<Attribute> attrs = updateState.getState(id);
              for (Attribute att : attrs) {
                // The value is actually going to be whether the door is closed
                // or not.
                Boolean open = BooleanConverter.get().decode(
                   att.getData());
                this.doorOpen = open.booleanValue();
                log.debug("{}: open? {}", id, open);
                // If the door is open, check each item
                if (open) {
                  missingItems = checkMissing();
                }
                // Door is closed now
                else {
                  this.resetAlert();
                }
              } // End Attributes
            } // End Identifiers

            if (missingItems != null && !missingItems.isEmpty()) {
              doAlert(missingItems.toArray(new String[missingItems.size()]));
            }

          } // End door response
          try {
            // Sleep because the interfaces used are polling-based.
            // Event-based is possible, but slightly more complex to code.
            Thread.sleep(50);
          } catch (InterruptedException ie) {
            // Why me worry?
          }
        }
      } catch (Exception e) {
        log.error("An error has occurred.", e);
      } finally {

        mobilityResponse.cancel();
        doorResponse.cancel();
      }
    }

    this.asClient.disconnect();
    this.asSolver.disconnect();
  }
  
  private Collection<String> checkMissing(){
    log.debug("Checking for missing items.");
    Collection<String> missingItems = new LinkedList<String>();
    boolean atLeastOneMoving = false;
    for (Entry<String, MobilityState> entry : itemMobility
        .entrySet()) {
      MobilityState state = entry.getValue();
      long timeSinceMobile = (System.currentTimeMillis() - state
          .getLastMobile()) / 1000;

      if (state.isMobile()
          || timeSinceMobile < this.config.getDelayToleranceSec()) {
        log.debug(entry.getKey() + ": moving? {} last time? {}", state.isMobile(), timeSinceMobile);
        log.debug("{} has been moving within {}s.",entry.getKey(),this.config.getDelayToleranceSec());
        atLeastOneMoving = true;
      } else {
        log.debug("{} is missing.",entry.getKey());
        missingItems.add(entry.getKey());
      }
    }
    if(atLeastOneMoving){
      return missingItems;
    }
    else{
      return null;
    }
  }

  public void shutdown() {
    this.keepRunning = false;
  }

  public void doAlert(String[] forgottenItems) {

    for (String s : forgottenItems) {
      Attribute attr = new Attribute();
      attr.setId(s);
      attr.setAttributeName(this.config.getAlertAttribute());
      attr.setCreationDate(System.currentTimeMillis());
      attr.setData(BooleanConverter.get().encode(Boolean.TRUE));
      this.asSolver.updateAttribute(attr);
    }
    
  }
  
  public void resetAlert(String item){
    Attribute attr = new Attribute();
    attr.setId(item);
    attr.setAttributeName(this.config.getAlertAttribute());
    attr.setCreationDate(System.currentTimeMillis());
    attr.setData(BooleanConverter.get().encode(Boolean.FALSE));
    this.asSolver.updateAttribute(attr);
  }
  
  public void resetAlert(){
    for(String s : this.config.getRequiredItems()){
      Attribute attr = new Attribute();
      attr.setId(s);
      attr.setAttributeName(this.config.getAlertAttribute());
      attr.setCreationDate(System.currentTimeMillis());
      attr.setData(BooleanConverter.get().encode(Boolean.FALSE));
      this.asSolver.updateAttribute(attr);
    }
  }

  public static String buildEitherOrRegex(final String[] options) {
    StringBuilder sb = new StringBuilder("^(");
    for (int i = 0; i < options.length; ++i) {
      if (i > 0) {
        sb.append("|");
      }
      sb.append(options[i]);
    }
    sb.append(")$");
    return sb.toString();
  }

}
