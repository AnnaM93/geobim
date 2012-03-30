package org.bimserver.citygml;

/******************************************************************************
 * Copyright (C) 2009-2012  BIMserver.org
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

/* TODO: Units
 */

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.apache.commons.beanutils.PropertyUtils;
import org.bimserver.citygml.xbuilding.GlobalIdType;
import org.bimserver.models.ifc2x3.IfcBoolean;
import org.bimserver.models.ifc2x3.IfcBuilding;
import org.bimserver.models.ifc2x3.IfcBuildingStorey;
import org.bimserver.models.ifc2x3.IfcColumn;
import org.bimserver.models.ifc2x3.IfcCurtainWall;
import org.bimserver.models.ifc2x3.IfcObject;
import org.bimserver.models.ifc2x3.IfcObjectDefinition;
import org.bimserver.models.ifc2x3.IfcPostalAddress;
import org.bimserver.models.ifc2x3.IfcProduct;
import org.bimserver.models.ifc2x3.IfcProject;
import org.bimserver.models.ifc2x3.IfcProperty;
import org.bimserver.models.ifc2x3.IfcPropertySet;
import org.bimserver.models.ifc2x3.IfcPropertySetDefinition;
import org.bimserver.models.ifc2x3.IfcPropertySingleValue;
import org.bimserver.models.ifc2x3.IfcRelContainedInSpatialStructure;
import org.bimserver.models.ifc2x3.IfcRelDecomposes;
import org.bimserver.models.ifc2x3.IfcRelDefines;
import org.bimserver.models.ifc2x3.IfcRelDefinesByProperties;
import org.bimserver.models.ifc2x3.IfcRoot;
import org.bimserver.models.ifc2x3.IfcSite;
import org.bimserver.models.ifc2x3.IfcSlab;
import org.bimserver.models.ifc2x3.IfcSlabTypeEnum;
import org.bimserver.models.ifc2x3.IfcSpace;
import org.bimserver.models.ifc2x3.IfcValue;
import org.bimserver.models.ifc2x3.IfcWall;
import org.bimserver.models.ifc2x3.Tristate;
import org.bimserver.plugins.PluginException;
import org.bimserver.plugins.PluginManager;
import org.bimserver.plugins.ifcengine.IfcEngine;
import org.bimserver.plugins.ifcengine.IfcEngineException;
import org.bimserver.plugins.ifcengine.IfcEngineGeometry;
import org.bimserver.plugins.ifcengine.IfcEngineInstance;
import org.bimserver.plugins.ifcengine.IfcEngineInstanceVisualisationProperties;
import org.bimserver.plugins.ifcengine.IfcEngineModel;
import org.bimserver.plugins.serializers.EmfSerializer;
import org.bimserver.plugins.serializers.IfcModelInterface;
import org.bimserver.plugins.serializers.ProjectInfo;
import org.bimserver.plugins.serializers.SerializerException;
import org.citygml4j.CityGMLContext;
import org.citygml4j.builder.jaxb.JAXBBuilder;
import org.citygml4j.factory.CityGMLFactory;
import org.citygml4j.factory.GMLFactory;
import org.citygml4j.factory.XALFactory;
import org.citygml4j.model.citygml.ade.ADEComponent;
import org.citygml4j.model.citygml.building.AbstractBoundarySurface;
import org.citygml4j.model.citygml.building.AbstractBuilding;
import org.citygml4j.model.citygml.building.Building;
import org.citygml4j.model.citygml.building.BuildingInstallation;
import org.citygml4j.model.citygml.building.BuildingPart;
import org.citygml4j.model.citygml.building.IntBuildingInstallation;
import org.citygml4j.model.citygml.building.Room;
import org.citygml4j.model.citygml.cityobjectgroup.CityObjectGroup;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.citygml.core.Address;
import org.citygml4j.model.citygml.core.CityModel;
import org.citygml4j.model.gml.base.AbstractGML;
import org.citygml4j.model.gml.basicTypes.Code;
import org.citygml4j.model.gml.feature.AbstractFeature;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurface;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;
import org.citygml4j.model.gml.geometry.complexes.CompositeSurface;
import org.citygml4j.model.gml.geometry.primitives.DirectPositionList;
import org.citygml4j.model.gml.geometry.primitives.LinearRing;
import org.citygml4j.model.gml.geometry.primitives.Polygon;
import org.citygml4j.model.module.citygml.CityGMLVersion;
import org.citygml4j.xml.io.CityGMLOutputFactory;
import org.citygml4j.xml.io.reader.CityGMLReadException;
import org.citygml4j.xml.io.writer.CityGMLWriteException;
import org.citygml4j.xml.io.writer.CityGMLWriter;
import org.eclipse.emf.ecore.EObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class CityGmlSerializer extends EmfSerializer {
	private static final Logger LOGGER = LoggerFactory.getLogger(CityGmlSerializer.class);
	private GMLFactory gml;
	private CityGMLFactory citygml;
	private CityGMLContext ctx;
	private IfcEngineModel ifcEngineModel;
	private IfcEngineGeometry geometry;
	private MaterialManager materialManager;	

	@Override
	public void init(IfcModelInterface ifcModel, ProjectInfo projectInfo, PluginManager pluginManager, IfcEngine ifcEngine) throws SerializerException {
		super.init(ifcModel, projectInfo, pluginManager, ifcEngine);
		
		ctx = new CityGMLContext();
		citygml = new CityGMLFactory();
		gml = new GMLFactory();
		new XALFactory();
		new HashMap<EObject, AbstractCityObject>();
		
		EmfSerializer serializer = getPluginManager().requireIfcStepSerializer();
		serializer.init(ifcModel, getProjectInfo(), getPluginManager(), ifcEngine);
		try {
			ifcEngine.init();
			ifcEngineModel = ifcEngine.openModel(serializer.getBytes());
			ifcEngineModel.setPostProcessing(true);
			geometry = ifcEngineModel.finalizeModelling(ifcEngineModel.initializeModelling());
		} catch (PluginException e) {
			throw new SerializerException(e);
		}
	}

	private Code createCode(String value) {
		Code code = gml.createCode();
		code.setValue(value);
		return code;
	}

	private Code createCode(String value, String codeSpace) {
		Code code = gml.createCode();
		code.setValue(value);
		code.setCodeSpace(codeSpace);
		return code;
	}
	
	private List<Code> createCodeList(String... values) {
		List<Code> list = new LinkedList<Code>();
		for(String value : values) {
			list.add(createCode(value));
		}
		return list;
	}
	
	private void setName(List<Code> name, String value) {
		if (value != null && !value.trim().equals("")) {
			name.add(createCode(value));
		}
	}
	
	// TODO: Put in utility class (also used in material manager)
	private String hrefTo(AbstractGML abstractGML) {
		if(abstractGML.getId() == null) {
			abstractGML.setId(UUID.randomUUID().toString());
		}
		return "#" + abstractGML.getId();
	}
		
	/**
	 * TODO: What does this do?
	 * @param cityObject
	 * @param ifcRoot
	 */
	private void setGlobalId(AbstractCityObject cityObject, IfcRoot ifcRoot) {
		cityObject.setId(UUID.randomUUID().toString());

		if (ifcRoot != null && ifcRoot.getGlobalId() != null && ifcRoot.getGlobalId().getWrappedValue() != null) {
			GlobalIdType globalId = new GlobalIdType();
			globalId.setValue(ifcRoot.getGlobalId().getWrappedValue());
						
			for (Element element : globalId.get_ADEComponent()) {
				ADEComponent adeComponent = new ADEComponent(element);
				cityObject.addGenericApplicationPropertyOfCityObject(adeComponent);
			}
		}
		
	}
	
	private void assignGuid(AbstractFeature cityObject, IfcRoot ifcRoot) {
		// TODO: Implement this
		
		// Use the ifc uuid's as gml:id's
		// Or use the xbuilding ADE to assign the uuid's?
		// Should we decode the ifc uuid's to normal onces?
		
		if(ifcRoot != null && ifcRoot.getGlobalId() != null && ifcRoot.getGlobalId().getWrappedValue() != null) {
			String ifcUuid = ifcRoot.getGlobalId().getWrappedValue();
			System.out.println("UUID: " + ifcUuid);
		}
	}
	
	@Override
	protected void reset() {
		setMode(Mode.BODY);
	}
	
	@Override
	public boolean write(OutputStream out) throws SerializerException {
		if(getMode() == Mode.BODY) {
			try {
				CityModel cityModel = buildCityModel();
				writeCityGML(cityModel, out);
				
				return true;
			} catch (Exception e) {
				throw new SerializerException(e);
			} finally {
				setMode(Mode.FINISHED);
				getIfcEngine().close();			
			}
		}
		
		return false;
	}
		
	/**
	 * Write given citymodel to given OutputStream.
	 */
	private void writeCityGML(CityModel cityModel, OutputStream out) throws JAXBException, CityGMLReadException, CityGMLWriteException {
		JAXBBuilder builder = ctx.createJAXBBuilder(getClass().getClassLoader());
		CityGMLOutputFactory outputFactory = builder.createCityGMLOutputFactory(CityGMLVersion.v1_0_0);
		PrintWriter writer = new PrintWriter(out);
		CityGMLWriter cityGmlWriter = outputFactory.createCityGMLWriter(writer);

		cityGmlWriter.setPrefixes(CityGMLVersion.v1_0_0);
		cityGmlWriter.setSchemaLocations(CityGMLVersion.v1_0_0);
		cityGmlWriter.setIndentString("  ");
		cityGmlWriter.write(cityModel);
		cityGmlWriter.close();
		writer.flush();
	}
	
	/**
	 * Build up a CityModel from the ifcModel and return it.
	 * @throws PluginException 
	 * @throws SerializerException 
	 */
	private CityModel buildCityModel() throws PluginException, SerializerException {
		IfcModelInterface ifcModel = getModel();
		
		CityModel cityModel = citygml.createCityModel();
		cityModel.setName(createCodeList(ifcModel.getName()));
		
		materialManager = new MaterialManager(cityModel, citygml);

		for(IfcProject ifcProject : getModel().getAll(IfcProject.class)) {
			buildCityModel(cityModel, ifcProject);
			break; // Since there is only one ifcProject per ifc file
		}
			
		return cityModel;
	}
		
	/**
	 * Pre: ifcEngine field must be initialized
	 * @param cityModel the base of the citygml data structure
	 * @param ifcProject the base of the ifc project
	 * @throws SerializerException 
	 */
	private void buildCityModel(CityModel cityModel, IfcProject ifcProject) throws SerializerException {
		assignGuid(cityModel, ifcProject);
		
		for(IfcRelDecomposes ifcRelDecomposes : ifcProject.getIsDecomposedBy()) {
			for(IfcObjectDefinition ifcObjectDefinition: ifcRelDecomposes.getRelatedObjects()) {
				if(ifcObjectDefinition instanceof IfcBuilding) {
					Building building = buildBuildingOrBuildingPart(cityModel, (IfcBuilding)ifcObjectDefinition, citygml.createBuilding());
					if(building != null) {
						cityModel.addCityObjectMember(citygml.createCityObjectMember(building));
					}
				}
				else if(ifcObjectDefinition instanceof IfcSite) {
					CityObjectGroup site = buildSite(cityModel, (IfcSite)ifcObjectDefinition);
					if(site != null) {
						cityModel.addCityObjectMember(citygml.createCityObjectMember(site));
					}
				}
				else {
					LOGGER.warn("Unhandled object in city model: " + ifcObjectDefinition);
				}
			}
		}
	}

	private CityObjectGroup buildSite(CityModel cityModel, IfcSite ifcSite) throws SerializerException {
		CityObjectGroup site = citygml.createCityObjectGroup();
		site.setClazz("site");
		setName(site.getName(), ifcSite.getName());
		setGlobalId(site, ifcSite);

		for(IfcRelDecomposes ifcRelDecomposes : ifcSite.getIsDecomposedBy()) {
			for(IfcObjectDefinition ifcObjectDefinition: ifcRelDecomposes.getRelatedObjects()) {
				if(ifcObjectDefinition instanceof IfcBuilding) {
					Building building = buildBuildingOrBuildingPart(cityModel, (IfcBuilding)ifcObjectDefinition, citygml.createBuilding());
					if(building != null) {
						site.addGroupMember(citygml.createCityObjectGroupMember(building));
					}
				}
				else if(ifcObjectDefinition instanceof IfcSite) {
					CityObjectGroup subSite = buildSite(cityModel, (IfcSite)ifcObjectDefinition);
					if(site != null) {
						site.addGroupMember(citygml.createCityObjectGroupMember(subSite));
					}
				}
				else {
					//TODO: Handle rooms at this level
					LOGGER.warn("Unhandled object in site: " + ifcObjectDefinition);
				}
			}
		}		
		
		return site;
	}

	private <T extends AbstractBuilding> T buildBuildingOrBuildingPart(CityModel cityModel, IfcBuilding ifcBuilding, T abstractBuilding) throws SerializerException {
		setName(abstractBuilding.getName(), ifcBuilding.getName());
		setGlobalId(abstractBuilding, ifcBuilding);

		// set address
		IfcPostalAddress ifcBuildingAddress = ifcBuilding.getBuildingAddress();
		if (ifcBuildingAddress != null) {
			abstractBuilding.addAddress(citygml.createAddressProperty(buildAddress(ifcBuildingAddress)));
		}
		
		// Handle all products on this level
		for (IfcRelContainedInSpatialStructure ifcRelContainedInSpatialStructure : ifcBuilding.getContainsElements()) {
			for (IfcProduct ifcProduct : ifcRelContainedInSpatialStructure.getRelatedElements()) {
				LOGGER.warn("Unhandled product in building or buildingpart: " + ifcProduct);
			}
		}
				
		// Continue with the spatial decomposition
		CityObjectGroup buildingStoreys = null;
				
		for (IfcRelDecomposes ifcRelDecomposes : ifcBuilding.getIsDecomposedBy()) {
			for (IfcObjectDefinition ifcObjectDefinition : ifcRelDecomposes.getRelatedObjects()) {
				if (ifcObjectDefinition instanceof IfcBuildingStorey) {
					if(buildingStoreys == null) {
						buildingStoreys = citygml.createCityObjectGroup();
						buildingStoreys.setClazz("building storeys");
						buildingStoreys.setGroupParent(citygml.createCityObjectGroupParent(hrefTo(abstractBuilding)));
						cityModel.addCityObjectMember(citygml.createCityObjectMember(buildingStoreys));
					}
					
					buildBuildingStorey(buildingStoreys, abstractBuilding, (IfcBuildingStorey)ifcObjectDefinition);
				}
				else if (ifcObjectDefinition instanceof IfcBuilding) {
					BuildingPart buildingPart = buildBuildingOrBuildingPart(cityModel, (IfcBuilding)ifcObjectDefinition, citygml.createBuildingPart());
					abstractBuilding.addConsistsOfBuildingPart(citygml.createBuildingPartProperty(buildingPart));
				}
				else {
					LOGGER.warn("Unhandled building or builingpart decomposition: " + ifcObjectDefinition);
				}
			}
		}
				
		return abstractBuilding;
	}
		
	private Address buildAddress(IfcPostalAddress ifcBuildingAddress) {
		Address address = citygml.createAddress();
		//AddressDetails details = xal.createAddressDetails();
		//TODO: Really create the address here
		return address;
	}

	private void buildBuildingStorey(CityObjectGroup buildingStoreys, AbstractBuilding abstractBuilding, IfcBuildingStorey ifcBuildingStorey) throws SerializerException {
		System.out.println("BuildingStorey: " + ifcBuildingStorey);
		
		CityObjectGroup buildingSeparation = citygml.createCityObjectGroup();
		buildingSeparation.setClazz("building separation");
		buildingSeparation.addFunction("lod4Strorey");
		setName(buildingSeparation.getName(), ifcBuildingStorey.getName());
		setGlobalId(buildingSeparation, ifcBuildingStorey);
				
		buildingStoreys.addGroupMember(citygml.createCityObjectGroupMember(buildingSeparation));

		// Loop through spaces (spacial structure)
		for (IfcRelDecomposes ifcRelDecomposes : ifcBuildingStorey.getIsDecomposedBy()) {
			for (IfcObjectDefinition ifcObjectDefinition : ifcRelDecomposes.getRelatedObjects()) {
				if (ifcObjectDefinition instanceof IfcSpace) {
					IfcSpace ifcSpace = (IfcSpace) ifcObjectDefinition;
					buildRoom(buildingSeparation, ifcSpace);
				}
				else {
					System.out.println("- " + ifcObjectDefinition + " (Spacial Structure)");
				}
			}
		}

		// Loop trough the products
		for (IfcRelContainedInSpatialStructure ifcRelContainedInSpatialStructure : ifcBuildingStorey.getContainsElements()) {
			for (IfcProduct ifcProduct : ifcRelContainedInSpatialStructure.getRelatedElements()) {
				// TODO: Handle products here
				AbstractBoundarySurface surface = null;
				
				if(ifcProduct instanceof IfcSlab) {
					IfcSlab ifcSlab = (IfcSlab) ifcProduct;
					
					if(ifcSlab.getPredefinedType() == IfcSlabTypeEnum.FLOOR) {
						surface = buildBoundarySurface(ifcSlab, citygml.createFloorSurface());
					}
					else if(ifcSlab.getPredefinedType() == IfcSlabTypeEnum.ROOF) {
						surface = buildBoundarySurface(ifcSlab, citygml.createRoofSurface());
					}					
					else if(ifcSlab.getPredefinedType() == IfcSlabTypeEnum.BASESLAB || ifcSlab.getPredefinedType() == IfcSlabTypeEnum.LANDING) {
						surface = buildBoundarySurface(ifcSlab, citygml.createGroundSurface());
					}										
				}
				else if(ifcProduct instanceof IfcWall) {
					if(getPropertySingleValue(ifcProduct, "Pset_WallCommon", "IsExternal", true)) {
						surface = buildBoundarySurface(ifcProduct, citygml.createWallSurface());
					}
					else {
						surface = buildBoundarySurface(ifcProduct, citygml.createInteriorWallSurface());
					}
				}
				else if(ifcProduct instanceof IfcColumn) {
					if(getPropertySingleValue(ifcProduct, "Pset_ColumnCommon", "IsExternal", false)) {
						BuildingInstallation buildingInstallation = buildBoundarySurface(ifcProduct, citygml.createBuildingInstallation());
						buildingInstallation.addFunction("1050");
						abstractBuilding.addOuterBuildingInstallation(citygml.createBuildingInstallationProperty(buildingInstallation));
						buildingSeparation.addGroupMember(citygml.createCityObjectGroupMember(hrefTo(buildingInstallation)));
					}
					else {
						IntBuildingInstallation intBuildingInstallation = buildBoundarySurface(ifcProduct, citygml.createIntBuildingInstallation());
						intBuildingInstallation.addFunction("1050");
						abstractBuilding.addInteriorBuildingInstallation(citygml.createIntBuildingInstallationProperty(intBuildingInstallation));
						buildingSeparation.addGroupMember(citygml.createCityObjectGroupMember(hrefTo(intBuildingInstallation)));
					}
				}
				else if(ifcProduct instanceof IfcCurtainWall) {
					// TODO: Add information to mark this as a CurtainWall in CityGML (does not exist)
					if(getPropertySingleValue(ifcProduct, "Pset_CurtainWallCommon", "IsExternal", true)) {
						surface = buildBoundarySurface(ifcProduct, citygml.createWallSurface());
					}
					else {
						surface = buildBoundarySurface(ifcProduct, citygml.createInteriorWallSurface());
					}
				}
				else {
					System.out.println("- unhandled " + ifcProduct + " (Product)");
				}
				
				// We added a boundary surface, so let's add it and check for voids and add doors and windows, etc.
				if(surface != null) {
					// Adding it
					abstractBuilding.addBoundedBySurface(citygml.createBoundarySurfaceProperty(surface));
					buildingSeparation.addGroupMember(citygml.createCityObjectGroupMember(hrefTo(surface)));
					
					// TODO: Handle voids with contents
				}
			}
		}	
		
		System.out.println("End BuildingStorey");
	}
	
	/**
	 * Retreivee a property from an IfcObject.
	 * @param propertySetName
	 * @param propertyName
	 * @return
	 * TODO: Determine Exception class for mismatched property class
	 */
	private IfcValue getPropertySingleValue(IfcObject ifcObject, String propertySetName, String propertyName) {
		for(IfcRelDefines ifcRelDefines : ifcObject.getIsDefinedBy()) {
			if(ifcRelDefines instanceof IfcRelDefinesByProperties) {
				IfcPropertySetDefinition ifcPropertySetDefinition = ((IfcRelDefinesByProperties)ifcRelDefines).getRelatingPropertyDefinition();
				if(ifcPropertySetDefinition instanceof IfcPropertySet && ifcPropertySetDefinition.getName().equals(propertySetName)) {
					IfcPropertySet ifcPropertySet = (IfcPropertySet) ifcPropertySetDefinition;
					
					for(IfcProperty ifcProperty : ifcPropertySet.getHasProperties()) {
						if(ifcProperty.getName().equals(propertyName)) {
							if(!(ifcProperty instanceof IfcPropertySingleValue)) throw new RuntimeException("PropertySingleValue expected");
							return ((IfcPropertySingleValue)ifcProperty).getNominalValue();							
						}
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the result of a boolean property. If the property does not exists the defaultValue will be returned.
	 * @param ifcObject
	 * @param propertySetName
	 * @param propertyName
	 * @return
	 */
	private boolean getPropertySingleValue(IfcObject ifcObject, String propertySetName, String propertyName, boolean defaultValue) {
		IfcValue ifcValue = getPropertySingleValue(ifcObject, propertySetName, propertyName);
		if(ifcValue == null) {
			return defaultValue;
		}
		else if(ifcValue instanceof IfcBoolean) {
			return ((IfcBoolean)ifcValue).getWrappedValue() == Tristate.TRUE; 
		}
		else {
			throw new RuntimeException("IfcBoolean property expected");
		}
	}

	private Room buildRoom(CityObjectGroup buildingSeparation, IfcSpace ifcSpace) {
		System.out.println("Room: " + ifcSpace);
		
		for (IfcRelDecomposes ifcRelDecomposes : ifcSpace.getIsDecomposedBy()) {
			for (IfcObjectDefinition ifcObjectDefinition : ifcRelDecomposes.getRelatedObjects()) {
				System.out.println("* " + ifcObjectDefinition);
			}
		}
		
		for (IfcRelContainedInSpatialStructure ifcRelContainedInSpatialStructure : ifcSpace.getContainsElements()) {
			for (IfcProduct ifcProduct : ifcRelContainedInSpatialStructure.getRelatedElements()) {
				System.out.println("- "  + ifcProduct);
			}
		}
		
		System.out.println("End Room");
		
		return null;
	}

	private <T extends AbstractCityObject> T buildBoundarySurface(IfcProduct ifcProduct, T cityObject) throws SerializerException {
		setName(cityObject.getName(), ifcProduct.getName());
		setGlobalId(cityObject, ifcProduct);
		
		MultiSurface multiSurface = gml.createMultiSurface();
		
		{
			CompositeSurface compositeSurface = gml.createCompositeSurface();			
			setGeometry(compositeSurface, ifcProduct);
			materialManager.assign(compositeSurface, ifcProduct);
			multiSurface.addSurfaceMember(gml.createSurfaceProperty(compositeSurface));
		}
		
		LinkedList<IfcObjectDefinition> decompose = new LinkedList<IfcObjectDefinition>(Collections.singletonList(ifcProduct));
		while(!decompose.isEmpty()) {
			for(IfcRelDecomposes ifcRelDecomposes: decompose.removeFirst().getIsDecomposedBy()) {
				for(IfcObjectDefinition ifcObjectDef : ifcRelDecomposes.getRelatedObjects()) {
					CompositeSurface compositeSurface = gml.createCompositeSurface();
					setGeometry(compositeSurface, ifcObjectDef);
					materialManager.assign(compositeSurface, ifcObjectDef);
					multiSurface.addSurfaceMember(gml.createSurfaceProperty(compositeSurface));
					decompose.add(ifcObjectDef);
				}
			}
		}
		
		MultiSurfaceProperty multiSurfaceProperty = gml.createMultiSurfaceProperty(multiSurface);
		try {
			if(PropertyUtils.isWriteable(cityObject, "lod4MultiSurface")) {
				PropertyUtils.setProperty(cityObject, "lod4MultiSurface", multiSurfaceProperty);
			}
			else {
				PropertyUtils.setProperty(cityObject, "lod4Geometry", multiSurfaceProperty);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
				
		return cityObject;
	}
	
	private void setGeometry(CompositeSurface ms, IfcRoot ifcRootObject) throws SerializerException {
		setGeometry(null, ms, ifcRootObject);
	}
	
	private void setGeometry(MultiSurface ms1, CompositeSurface ms2, IfcRoot ifcRootObject) throws SerializerException {
		try {
			IfcEngineInstance instance = ifcEngineModel.getInstanceFromExpressId((int) ifcRootObject.getOid());
			IfcEngineInstanceVisualisationProperties instanceInModelling = instance.getVisualisationProperties();
			for (int i = instanceInModelling.getStartIndex(); i < instanceInModelling.getPrimitiveCount() * 3 + instanceInModelling.getStartIndex(); i += 3) {
				int i1 = geometry.getIndex(i) * 3;
				int i2 = geometry.getIndex(i + 1) * 3;
				int i3 = geometry.getIndex(i + 2) * 3;
				
				DirectPositionList posList = gml.createDirectPositionList();
				posList.setSrsDimension(3);
				posList.setValue(Arrays.asList(new Double[] { 
						(double) geometry.getVertex(i1 + 0), (double) geometry.getVertex(i1 + 1), (double) geometry.getVertex(i1 + 2),
						(double) geometry.getVertex(i3 + 0), (double) geometry.getVertex(i3 + 1), (double) geometry.getVertex(i3 + 2),
						(double) geometry.getVertex(i2 + 0), (double) geometry.getVertex(i2 + 1), (double) geometry.getVertex(i2 + 2),
						(double) geometry.getVertex(i1 + 0), (double) geometry.getVertex(i1 + 1), (double) geometry.getVertex(i1 + 2) 
				}));
				
				LinearRing linearRing = gml.createLinearRing();
				linearRing.setPosList(posList);
				
				Polygon polygon = gml.createPolygon();
				polygon.setExterior(gml.createExterior(linearRing));
							
				if(ms1 != null) {
					ms1.addSurfaceMember(gml.createSurfaceProperty(polygon));
				}
				else if(ms2 != null){
					ms2.addSurfaceMember(gml.createSurfaceProperty(polygon));
				}
			}
		} catch (IfcEngineException e) {
			throw new SerializerException("IfcEngineException", e);
		} catch (Exception e) {
			LOGGER.error("", e);
		}
	}
	
}