package nl.tue.bimserver.citygml;

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
