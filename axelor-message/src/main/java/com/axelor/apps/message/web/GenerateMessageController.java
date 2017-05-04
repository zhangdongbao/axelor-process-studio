/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2017 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.message.web;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.base.db.Wizard;
import com.axelor.apps.message.db.Message;
import com.axelor.apps.message.db.Template;
import com.axelor.apps.message.db.repo.TemplateRepository;
import com.axelor.apps.message.exception.IExceptionMessage;
import com.axelor.apps.message.service.TemplateMessageService;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.exception.AxelorException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.inject.Inject;

public class GenerateMessageController {

	@Inject
	private TemplateMessageService templateMessageService;
	
	@Inject
	private TemplateRepository templateRepo;

	private static final Logger LOG = LoggerFactory.getLogger(GenerateMessageController.class);
	
	public void callMessageWizard(ActionRequest request, ActionResponse response)   {
		
		Model context = request.getContext().asType( Model.class );
		String model = request.getModel();
		
		LOG.debug("Call message wizard for model : {} ", model);
		
		String[] decomposeModel = model.split("\\.");
		String simpleModel = decomposeModel[ decomposeModel.length - 1 ];
		
		Query<? extends Template> templateQuery = templateRepo.all().filter("self.metaModel.fullName = ?1 AND self.isSystem != true",  model );
		
		try {		
		
			long templateNumber = templateQuery.count();
			
			LOG.debug("Template number : {} ", templateNumber);
			
			if ( templateNumber == 0 )  { response.setFlash(I18n.get(IExceptionMessage.MESSAGE_1)); }
			
			else if ( templateNumber > 1 || templateNumber == 0 )  {
	
				response.setView( 
						ActionView.define( I18n.get( IExceptionMessage.MESSAGE_2 ) )
						.model(Wizard.class.getName())
						.add("form", "generate-message-wizard-form")
						.context( "_objectId", context.getId().toString() )
						.context( "_templateContextModel", model )
						.context( "_tag", simpleModel )
						.map()
					);
				
			} else  {
				response.setView( generateMessage( context.getId(), model, simpleModel, templateQuery.fetchOne() ) );
			}
			
		} catch(Exception e)  { TraceBackService.trace(response, e); }
	}
	
	
	
	public void generateMessage(ActionRequest request, ActionResponse response)  {
		
		Context context = request.getContext();
		Map<?,?> templateContext = (Map<?,?>) context.get("_xTemplate");
		Template template = templateRepo.find( Long.parseLong( templateContext.get("id").toString() ) );
		
		Long objectId =  Long.parseLong( context.get("_objectId").toString() );
		String model = (String) context.get("_templateContextModel");
		String tag = (String) context.get("_tag");

		try { response.setView( generateMessage( objectId, model, tag, template ) ); } 
		catch(Exception e)  { TraceBackService.trace(response, e); }
	}
	
	
	public Map<String,Object> generateMessage(long objectId, String model, String tag, Template template) throws SecurityException, NoSuchFieldException, ClassNotFoundException, InstantiationException, IllegalAccessException, AxelorException, IOException  {
		
		LOG.debug("template : {} ", template);
		LOG.debug("object id : {} ", objectId);
		LOG.debug("model : {} ", model);
		LOG.debug("tag : {} ", tag);
		
		Message message = templateMessageService.generateMessage(objectId, model, tag, template);

		return ActionView.define( I18n.get(IExceptionMessage.MESSAGE_3) )
				.model( Message.class.getName() )
				.add("form", "message-form")
				.context("_showRecord", message.getId().toString() )
				.map();
			
	}
	
	
}
