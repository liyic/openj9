/*******************************************************************************
 * Copyright (c) 2001, 2014 IBM Corp. and others
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 * or the Apache License, Version 2.0 which accompanies this distribution and
 * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * This Source Code may also be made available under the following
 * Secondary Licenses when the conditions for such availability set
 * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 * General Public License, version 2 with the GNU Classpath
 * Exception [1] and GNU General Public License, version 2 with the
 * OpenJDK Assembly Exception [2].
 *
 * [1] https://www.gnu.org/software/classpath/license.html
 * [2] http://openjdk.java.net/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/
package com.ibm.j9ddr.vm29.tools.ddrinteractive.commands;

import java.io.PrintStream;

import com.ibm.j9ddr.CorruptDataException;
import com.ibm.j9ddr.tools.ddrinteractive.CommandUtils;
import com.ibm.j9ddr.tools.ddrinteractive.Command;
import com.ibm.j9ddr.tools.ddrinteractive.Context;
import com.ibm.j9ddr.tools.ddrinteractive.DDRInteractiveCommandException;
import com.ibm.j9ddr.vm29.j9.DataType;
import com.ibm.j9ddr.vm29.pointer.U8Pointer;
import com.ibm.j9ddr.vm29.pointer.UDATAPointer;
import com.ibm.j9ddr.vm29.pointer.generated.J9BuildFlags;
import com.ibm.j9ddr.vm29.pointer.generated.J9ClassPointer;
import com.ibm.j9ddr.vm29.pointer.generated.J9JavaVMPointer;
import com.ibm.j9ddr.vm29.pointer.generated.J9MethodPointer;
import com.ibm.j9ddr.vm29.pointer.helper.J9ClassHelper;
import com.ibm.j9ddr.vm29.pointer.helper.J9MethodHelper;
import com.ibm.j9ddr.vm29.pointer.helper.J9RASHelper;
import com.ibm.j9ddr.vm29.types.UDATA;

public class J9VTablesCommand extends Command 
{
	public J9VTablesCommand()
	{
		addCommand("j9vtables", "<ramclass>", "dump interpreter and jit vtables for given ram class.");
	}
	

	public void run(String command, String[] args, Context context, final PrintStream out) throws DDRInteractiveCommandException 
	{
		try {
			final J9JavaVMPointer vm = J9RASHelper.getVM(DataType.getJ9RASPointer());
		    J9ClassPointer ramClass;
		   
		    UDATA vTableSlotCount;
//		    UDATA  jitVTableSlotCount;
		    UDATAPointer jitVTable = UDATAPointer.NULL;
		    UDATAPointer vTable;

		    long address = CommandUtils.parsePointer(args[0], J9BuildFlags.env_data64);
		    
		    ramClass = J9ClassPointer.cast(address);
		    
		    vTable = J9ClassHelper.vTable(ramClass);
		    vTableSlotCount = vTable.at(0);

			if(J9BuildFlags.interp_nativeSupport) {
			    if (!vm.jitConfig().isNull()) {
			        jitVTable = UDATAPointer.cast(U8Pointer.cast(ramClass).sub((vTableSlotCount.longValue()+1) * UDATA.SIZEOF));
//			        jitVTableSlotCount = vTableSlotCount.sub(1);
			    }
			}
	
			CommandUtils.dbgPrint(out, String.format("VTable for j9class %s  (size=%d - 1 for skipped resolve method)\n", ramClass.getHexAddress(), vTableSlotCount.longValue()));
			CommandUtils.dbgPrint(out, String.format("\tInterpreted%s\n", (!jitVTable.isNull()) ? "\t\tJitted\n" : ""));
			
			/*	First entry in vtable is the vtable count.
			 * 	Second entry is the magic method reference.
			 *  Skip both and start from the third element in vTable which is the first virtual method reference.
			 **/
		    for (long i = 2; i < vTableSlotCount.longValue() + 1; i++) {
		    	String name = J9MethodHelper.getName(J9MethodPointer.cast(vTable.at(i)));		    	
		    	String intAddr = U8Pointer.cast(vTable.at(i)).getHexAddress();
		        if (!jitVTable.isNull()) {		            
		        	String jitAddr = U8Pointer.cast(jitVTable.at(vTableSlotCount.sub(i))).getHexAddress();
		        	CommandUtils.dbgPrint(out, String.format(" %d\t!j9method %s\t%s\t%s\n", i-1, intAddr, jitAddr, name));
		        } else {
		        	CommandUtils.dbgPrint(out, String.format(" %d\t!j9method %s\t%s\t\n", i-1, intAddr, name));
		        }
		    }
		    return;
		} catch (CorruptDataException e) {
			throw new DDRInteractiveCommandException(e);
		}
	}
}
