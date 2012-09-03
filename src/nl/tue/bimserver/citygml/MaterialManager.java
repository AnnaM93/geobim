package nl.tue.bimserver.citygml;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bimserver.models.ifc2x3tc1.IfcObjectDefinition;
import org.bimserver.models.ifc2x3tc1.IfcPlate;
import org.citygml4j.factory.CityGMLFactory;
import org.citygml4j.model.citygml.appearance.Appearance;
import org.citygml4j.model.citygml.appearance.Color;
import org.citygml4j.model.citygml.appearance.X3DMaterial;
import org.citygml4j.model.citygml.core.CityModel;
import org.citygml4j.model.gml.base.AbstractGML;
import org.citygml4j.model.gml.geometry.primitives.AbstractSurface;

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

class MaterialManager {
	private CityGMLFactory citygml;
	private Map<Class<?>,X3DMaterial> material = new HashMap<Class<?>,X3DMaterial>();
	private Appearance appearance;
	
	MaterialManager(CityModel cityModel, CityGMLFactory citygml) {
		this.citygml = citygml;
		appearance = citygml.createAppearance();
		appearance.setTheme("ifc2citygml");
		cityModel.addAppearanceMember(citygml.createAppearanceMember(appearance));
	
		createMaterial(IfcPlate.class, citygml.createColor(0.7, 0.7, 1.0), 0.9);
	}

	private void createMaterial(Class<?> klass, Color diffuseColor, double transparency) {
		X3DMaterial material = citygml.createX3DMaterial();
		material.setDiffuseColor(diffuseColor);
		material.setTransparency(transparency);
		appearance.addSurfaceDataMember(citygml.createSurfaceDataProperty(material));
		this.material.put(klass, material);
	}

	public void assign(AbstractSurface compositeSurface, IfcObjectDefinition ifcObjectDef) {
		for(Map.Entry<Class<?>,X3DMaterial> entry : material.entrySet()) {
			if(entry.getKey().isAssignableFrom(ifcObjectDef.getClass())) {
				entry.getValue().addTarget(hrefTo(compositeSurface));
			}
		}				
	}		
	
	private String hrefTo(AbstractGML abstractGML) {
		if(abstractGML.getId() == null) {
			abstractGML.setId(UUID.randomUUID().toString());
		}
		return "#" + abstractGML.getId();
	}	
}
