/*******************************************************************************
 * Copyright (c) 2015 AKSW Xturtle Project, itemis AG (http://www.itemis.eu).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package de.itemis.tooling.xturtle.ui.contentassist;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;

import de.itemis.tooling.xturtle.LexerOverrider;
import de.itemis.tooling.xturtle.ui.contentassist.antlr.internal.InternalXturtleLexer;

public class CustomXturtleContentassistLexer extends InternalXturtleLexer {

	LexerOverrider overrider=new LexerOverrider(CustomXturtleContentassistLexer.class);

	@Override
	public Token nextToken() {
		return super.nextToken();
	}

	@Override
	public void mTokens() throws RecognitionException {
		if(overrider.override(input, state)){
			//done
		}else{
			super.mTokens();
		}
	}
}
