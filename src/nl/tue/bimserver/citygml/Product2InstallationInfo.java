package nl.tue.bimserver.citygml;

/******************************************************************************
 * Copyright (C) 2012  Design Systems (www.ds.arch.tue.nl)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

public class Product2InstallationInfo {
	private String pSet;
	private String internalFunction;
	private String externalFunction;
	private boolean defaultExternal;

	public Product2InstallationInfo(String pSet, String internalFunction,
			String externalFunction, boolean defaultExternal) {
		super();
		this.pSet = pSet;
		this.internalFunction = internalFunction;
		this.externalFunction = externalFunction;
		this.defaultExternal = defaultExternal;
	}

	public String getpSet() {
		return pSet;
	}

	public void setpSet(String pSet) {
		this.pSet = pSet;
	}

	public String getInternalFunction() {
		return internalFunction;
	}

	public void setInternalFunction(String internalFunction) {
		this.internalFunction = internalFunction;
	}

	public String getExternalFunction() {
		return externalFunction;
	}

	public void setExternalFunction(String externalFunction) {
		this.externalFunction = externalFunction;
	}	
	
	public boolean isDefaultExternal() {
		return defaultExternal;
	}

	public void setDefaultExternal(boolean defaultExternal) {
		this.defaultExternal = defaultExternal;
	}
}
