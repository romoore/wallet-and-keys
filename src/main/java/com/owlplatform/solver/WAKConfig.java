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

/**
 * @author Robert Moore
 *
 */
public class WAKConfig {
  /**
   * Hostname of the world model server.
   */
  private String worldModelHost = "localhost";
  /**
   * Solver port of the world model (for sending solutions).
   */
  private int worldModelSolverPort = 7009;
  
  /**
   * Client port of the world model (for subscribing to data).
   */
  private int worldModelClientPort = 7010;
  
  /**
   * Array of Identifiers of objects to track for mobility together. Effectively a group of
   * items that should always be kept together when leaving the building.
   */
  private String[] requiredItems = null;
  
  /**
   * Array of Identifiers of doors we need to watch.  Make sure we have all of our required items
   * whenever one of the doors opens.
   */
  private String[] doors = null;
  
  /**
   * Array of attribute names that are associated with item mobility.
   */
  private String[] mobilityAttributeNames = new String[]{"mobility"};
  
  /**
   * Array of attribute names that are associated with doors being open or closed.
   */
  private String[] openDoorAttributeNames = new String[]{"closed"};

  public String getWorldModelHost() {
    return worldModelHost;
  }

  public void setWorldModelHost(String worldModelHost) {
    this.worldModelHost = worldModelHost;
  }

  public int getWorldModelSolverPort() {
    return worldModelSolverPort;
  }

  public void setWorldModelSolverPort(int worldModelSolverPort) {
    this.worldModelSolverPort = worldModelSolverPort;
  }

  public int getWorldModelClientPort() {
    return worldModelClientPort;
  }

  public void setWorldModelClientPort(int worldModelClientPort) {
    this.worldModelClientPort = worldModelClientPort;
  }

  public String[] getRequiredItems() {
    return requiredItems;
  }

  public void setRequiredItems(String[] requiredItems) {
    this.requiredItems = requiredItems;
  }

  public String[] getDoors() {
    return doors;
  }

  public void setDoors(String[] doors) {
    this.doors = doors;
  }
}
