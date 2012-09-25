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
 * A simple solver that tries to detect whether you've forgotten something on
 * your way out based on the mobility of your things and a door switch. If any
 * of the items listed as part of a group have not moved together within some
 * number of seconds before a door opens, then it produces an alert.
 * 
 * @author Robert Moore
 * 
 */
public class WalletAndKeys {

  /**
   * Parses the arguments and gets things started.
   * @param args <Config File>
   */
  public static void main(String[]args){
    if(args.length < 1){
      System.err.println("Missing one or more arguments:\n\t<Config File>");
      return;
    }
    
  }
  
}
