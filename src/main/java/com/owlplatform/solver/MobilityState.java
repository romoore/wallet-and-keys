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
public class MobilityState {
  private boolean mobile = false;
  private long lastMobile = 0l;
  public boolean isMobile() {
    return mobile;
  }
  public void setMobile(boolean mobile) {
    this.mobile = mobile;
  }
  public long getLastMobile() {
    return lastMobile;
  }
  public void setLastMobile(long lastMobile) {
    this.lastMobile = lastMobile;
  }
  
  
}
