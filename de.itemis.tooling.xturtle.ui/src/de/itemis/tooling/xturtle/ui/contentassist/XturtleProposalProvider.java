/*******************************************************************************
 * Copyright (c) 2013 AKSW Xturtle Project, itemis AG (http://www.itemis.eu).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
/*
* generated by Xtext
*/
package de.itemis.tooling.xturtle.ui.contentassist;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.ui.editor.contentassist.ConfigurableCompletionProposal;
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor;
import org.eclipse.xtext.ui.editor.contentassist.PrefixMatcher;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.inject.Inject;

import de.itemis.tooling.xturtle.resource.TurtleResourceService;
import de.itemis.tooling.xturtle.services.Prefixes;
import de.itemis.tooling.xturtle.services.XturtleGrammarAccess;
import de.itemis.tooling.xturtle.xturtle.BlankObjects;
import de.itemis.tooling.xturtle.xturtle.Object;
import de.itemis.tooling.xturtle.xturtle.PredicateObjectList;
import de.itemis.tooling.xturtle.xturtle.PrefixId;
import de.itemis.tooling.xturtle.xturtle.StringLiteral;
import de.itemis.tooling.xturtle.xturtle.Subject;
import de.itemis.tooling.xturtle.xturtle.Triples;
import de.itemis.tooling.xturtle.xturtle.XturtlePackage;
/**
 * see http://www.eclipse.org/Xtext/documentation/latest/xtext.html#contentAssist on how to customize content assistant
 */
public class XturtleProposalProvider extends AbstractXturtleProposalProvider {

	@Inject 
	TurtleResourceService service;
	@Inject
	XturtleGrammarAccess ga;

	@Inject
	Prefixes prefixes;
	
	@Inject
	TurtleLiteralsLanguages languages;

	//prefixMatcher only for ColonNames!!
	private PrefixMatcher subStringMatcher=new PrefixMatcher(){
		CamelCase delegate=new CamelCase();
		@Override
		public boolean isCandidateMatchingPrefix(String name, String prefix) {
			return name.toLowerCase().contains(prefix.substring(1).toLowerCase()) || delegate.isCandidateMatchingPrefix(name, prefix);
		}};

	private class LabelPrefixMatcher extends PrefixMatcher{

		private String label;
		public LabelPrefixMatcher(String forLabel) {
			label=forLabel.toLowerCase();
		}
		@Override
		public boolean isCandidateMatchingPrefix(String name, String prefix) {
			return label.contains(prefix.substring(1).toLowerCase());
		}
	}

	@Override
	protected String getDisplayString(EObject element,
			String qualifiedNameAsString, String shortName) {
		if(element instanceof PrefixId){
			String id=((PrefixId) element).getId();
			String displayprefix="";
			if(id!=null){
				displayprefix=id+" - ";
			}
			return displayprefix+((PrefixId) element).getUri();
		}
		return super.getDisplayString(element, qualifiedNameAsString, shortName);
	}

	@Override
	public void completeQNameRef_Ref(EObject model, Assignment assignment,
			final ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		String prefix=context.getPrefix();
		if(prefix==null || prefix.length()==0 || prefix.charAt(0)!=':'){
			return;
		}
		final ContentAssistContext newContext=context.copy().setMatcher(subStringMatcher).toContext();
		//the following seems to be the case if code completion is invoked on an empty prefix
		//in this case getting the previous model returns the actual node to be completed!?
		if(model instanceof PredicateObjectList){
			model=context.getPreviousModel();
		}
		final QualifiedName currentQName = service.getQualifiedName(model);
		final List<IEObjectDescription> additionalProposals=new ArrayList<IEObjectDescription>();
		ReferenceProposalCreator creator = getCrossReferenceProposalCreator();
		Predicate<IEObjectDescription>predicate=new Predicate<IEObjectDescription>() {
			public boolean apply(IEObjectDescription object){
				boolean nsMatches=object.getQualifiedName().getFirstSegment().equals(currentQName.getFirstSegment());
				if(nsMatches){
					String label=object.getUserData("label");
					if(label!=null){
						additionalProposals.add(object);
					}
				}
				return nsMatches;
			}
		};
		Function<IEObjectDescription, ICompletionProposal> factory = new Function<IEObjectDescription, ICompletionProposal>() {
			public ICompletionProposal apply(IEObjectDescription desc){
				return createCompletionProposal(":"+desc.getQualifiedName().getLastSegment(), newContext);
			}
		};
		
		creator.lookupCrossReference((EObject)model, XturtlePackage.Literals.RESOURCE_REF__REF, acceptor, predicate, factory);
		for (IEObjectDescription additional : additionalProposals) {
			String[] labels = additional.getUserData("label").split(",,");
			String name=additional.getQualifiedName().getLastSegment();
			String matchString="\"{1,3}"+name+"\"{1,3}";
			for (String label : labels) {
				if(!label.matches(matchString)){
					acceptor.accept(
							createCompletionProposal(
									":"+name, 
									new StyledString(label).append(" - ").append(name),
									null,
									100,
									context.getPrefix(),
									context.copy().setMatcher(new LabelPrefixMatcher(label)).toContext()));
				}
			}
		}
//		super.completeQNameRef_Ref(model, assignment, context, acceptor);
	}
	
	
	//complete URIs only if < has already been entered
	@Override
	public void complete_UriRef(EObject model, RuleCall ruleCall,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		String prefix=context.getPrefix();
		if(prefix==null || prefix.length()==0 || prefix.charAt(0)!='<'){
			return;
		}
		super.complete_UriRef(model, ruleCall, context, acceptor);
	}
	
	//complete URIs only if < has already been entered
	@Override
	public void completeUriRef_Ref(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		String prefix=context.getPrefix();
		if(prefix==null || prefix.length()==0 || prefix.charAt(0)!='<'){
			return;
		}
		super.completeUriRef_Ref(model, assignment, context, acceptor);
	}

	//fetch the most popular URI from prefix.cc
	@Override
	public void completePrefixId_Uri(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		if(model instanceof PrefixId){
			String id=((PrefixId)model).getId();
			List<String> uri=prefixes.getUris(id);
			if(uri!=null){
				acceptor.accept(createCompletionProposal("<"+uri.get(0)+">", context));
			}
		}
	}

	//propose prefix ids known to prefix.cc
	@Override
	public void completePrefixId_Id(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		for (String prefix : prefixes.getPrefixes()) {
			String proposal = prefix;//+":<"+prefixes.getUri(prefix)+">";
			acceptor.accept(createCompletionProposal(proposal,proposal+" - "+prefixes.getUris(prefix).get(0),null, context));
		}
	}

	private class ColonAddingAcceptor implements ICompletionProposalAcceptor{
		private ICompletionProposalAcceptor delegate;
		public ColonAddingAcceptor(ICompletionProposalAcceptor delegate) {
			this.delegate=delegate;
		}
		public void accept(ICompletionProposal proposal) {
			if(proposal instanceof ConfigurableCompletionProposal){
				ConfigurableCompletionProposal newProposal = (ConfigurableCompletionProposal) proposal;
				newProposal.setReplacementString(newProposal.getReplacementString()+":");
				newProposal.setReplaceContextLength(newProposal.getReplaceContextLength()+1);
				newProposal.setCursorPosition(newProposal.getCursorPosition()+1);
			}
			delegate.accept(proposal);
		}

		public boolean canAcceptMoreProposals() {
			return delegate.canAcceptMoreProposals();
		}
	}

	//colon is not part of the prefix, but we want to add it automatically
	//so the user does not have to do it, the implemented hack avoids another
	//colon to be added in case there already is one
	@Override
	public void completeQNameRef_Prefix(EObject model, Assignment assignment,
			ContentAssistContext context, final ICompletionProposalAcceptor acceptor) {
		ICompletionProposalAcceptor colonAddingAcceptor=acceptor;
		if(!context.getCurrentNode().getText().contains(":")){
			colonAddingAcceptor=new ColonAddingAcceptor(acceptor);
		}
		super.completeQNameRef_Prefix(model, assignment, context, colonAddingAcceptor);
	}
	@Override
	public void completeQNameDef_Prefix(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		ICompletionProposalAcceptor colonAddingAcceptor=acceptor;
		if(!context.getCurrentNode().getText().contains(":")){
			colonAddingAcceptor=new ColonAddingAcceptor(acceptor);
		}
		super.completeQNameDef_Prefix(model, assignment, context, colonAddingAcceptor);
	}

	
	@Override
	public void complete_StringLiteral(EObject model, RuleCall ruleCall,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		String prefix = context.getPrefix();
		if(prefix==null || prefix.length()==0 || prefix.charAt(0)!='"'){
			//code completion only if it is clear that a string literal is started
			return;
		}

		//propose string literals from the current file whose predicate URI matches that of the current predicate
		PredicateObjectList objList = EcoreUtil2.getContainerOfType(model, PredicateObjectList.class);
		if(objList!=null){
			de.itemis.tooling.xturtle.xturtle.Predicate verb = objList.getVerb();
			QualifiedName name = service.getQualifiedName(verb);
			if(name!=null){
				EObject root = EcoreUtil2.getRootContainer(model);
				List<de.itemis.tooling.xturtle.xturtle.Predicate> candidates = EcoreUtil2.getAllContentsOfType(root, de.itemis.tooling.xturtle.xturtle.Predicate.class);
				for (de.itemis.tooling.xturtle.xturtle.Predicate predicate : candidates) {
					if(name.equals(service.getQualifiedName(predicate))){
						EObject container = predicate.eContainer();
						if(container instanceof PredicateObjectList){
							EList<Object> objects = ((PredicateObjectList)predicate.eContainer()).getObjects();
							for (Object object : objects) {
								if(object instanceof StringLiteral){
									acceptor.accept(createCompletionProposal(((StringLiteral) object).getValue(), context));
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void completeStringLiteral_Language(EObject model,
			Assignment assignment, ContentAssistContext context,
			ICompletionProposalAcceptor acceptor) {
		for (String language : languages.getLanguagesToPropose()) {
			acceptor.accept(createCompletionProposal("@"+language, context));
		}
	}

	public void complete_TRIPELEND(Triples model, RuleCall ruleCall,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		boolean isAxiom=model.getSubject() instanceof BlankObjects;
		boolean predicateNonEmpty=!model.getPredObjs().isEmpty();
		boolean doPropose=false;
		if(isAxiom){
			doPropose=true;
		}else if(predicateNonEmpty){
			EObject semanticNode = NodeModelUtils.findActualSemanticObjectFor(context.getLastCompleteNode());
			if(!(semanticNode instanceof Subject)){
				doPropose=true;
			}
		}
		if(doPropose){
			super.completeKeyword(ga.getNameAccess().getFullStopKeyword_1_0_0(), context, acceptor);
		}
	}

	@Override
	public void completeKeyword(Keyword keyword,
			ContentAssistContext contentAssistContext,
			ICompletionProposalAcceptor acceptor) {
		char firstCharacter=keyword.getValue().charAt(0);
		if(firstCharacter!='.'){
			boolean wsBeforeKeywordRequired;
			switch (firstCharacter){
			case ',':
			case ':':
			case ';':wsBeforeKeywordRequired=false;
			break;
			default: wsBeforeKeywordRequired=true;
			}

			int offset = contentAssistContext.getOffset();
			int lastNodeEndOffset=contentAssistContext.getLastCompleteNode().getTotalEndOffset();
			boolean startOfDocument=offset-contentAssistContext.getPrefix().length()==0;
			boolean wsAfterLastCompleteNode=startOfDocument || offset>lastNodeEndOffset;

			if(!wsBeforeKeywordRequired || wsAfterLastCompleteNode){
				super.completeKeyword(keyword, contentAssistContext, acceptor);
			}
		}
	}
}
