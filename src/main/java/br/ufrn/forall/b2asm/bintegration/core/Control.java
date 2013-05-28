package br.ufrn.forall.b2asm.bintegration.core;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import br.ufrn.forall.b2asm.bintegration.core.Report.POWD;
import br.ufrn.forall.b2asm.bintegration.core.Report.PoGenerated;
import br.ufrn.forall.b2asm.bintegration.core.StreamGobbler.Result;
import br.ufrn.forall.b2asm.bintegration.pos.POs;

/**
 * This class contains the control elements to call the ProB evaluator
 * 
 * @author valerio
 * 
 */

public class Control {

	Result result;
	String currentPath, executablePath;
	StringBuffer command;

	String expressionName;
	String moduleName;
	String modulePath;
	String pathBModuleInBdpFolderWithoutExtension;
	String separetor;
	String componentExtension;
	String goal; //
	String hypothesis;
	boolean isWD;
	long total_time = 0;

	final int indExpressionName = 0;
	final int indModuleName = 1;
	final int indModulePath = 2;
	final int indComponentExtension = 3;
	final int indGoal = 4; //
	private int exitVal;
	private POs expressionsToEvaluate;

	public Control() {

	}

	/**
	 * This method load from a file config.txt: the path of ProB; the time limit and others informations.
	 * 
	 */
	void loadConfig() {

		try {

			command = new StringBuffer();
			// currentPath = new java.io.File( "." ).getCanonicalPath();
			currentPath = Control.class.getProtectionDomain().getCodeSource()
					.getLocation().getPath()
					.replaceAll(Installation.filenameJar, "");
			System.out.println("CurrentPath:" + currentPath);
			FileInputStream fstream = new FileInputStream(currentPath
					+ File.separator + "Config.txt");

			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String strLine;
			boolean first = true;
			while (((strLine = br.readLine()) != null)) {

				if (!strLine.contains("#")) {

					if (first) {
						executablePath = strLine;
						first = false;
					} else
						command.append(strLine + " \n");
				}

			}

			in.close();

		} catch (IOException e) {

			System.err
					.println("Error - B2asm: it is not possible find the current path.");

			e.printStackTrace();
		}

	}

	public String getExecutablePath() {
		return executablePath;
	}

	public void setIndividualArgs(String[] args) {
		boolean startReadHypothesis = false;
		StringBuffer tmpGoal = new StringBuffer();
		StringBuffer tmpHypothesis = new StringBuffer();
		try {
			expressionName = args[indExpressionName];
			moduleName = args[indModuleName];
			modulePath = args[indModulePath];
			componentExtension = args[indComponentExtension];

		} catch (Exception e) {

			System.err.println("Error: you need specify the parameters");

		}

		int cont = indGoal;

		for (; cont < args.length; cont++) {

			if (!startReadHypothesis && !args[cont].contains("#@#"))
				tmpGoal.append(" " + args[cont]);
			else
				break;

		}
		// start the read the next element of hypothesis
		for (cont++; cont < args.length; cont++)
			tmpHypothesis.append(" " + args[cont]);

		// Remove description text on goal PO
		goal = tmpGoal.toString().replaceAll("\"(.*?)\"", "btrue");

		// Remove description tex on hypothesis PO
		hypothesis = tmpHypothesis.toString().replaceAll("\"(.*?)\"", "btrue");

	}


	public void setModuleArgs(String[] args) {
		
		boolean startReadHypothesis = false;
		StringBuffer tmpGoal = new StringBuffer();
		StringBuffer tmpHypothesis = new StringBuffer();
		try {
			expressionName = args[indExpressionName];
			moduleName = args[indModuleName];
			modulePath = args[indModulePath];
			componentExtension = args[indComponentExtension];
			
			//TODO: To use the variable ${projectBdp}from AtelierB that contains the path to the bdp directory of the project.
			pathBModuleInBdpFolderWithoutExtension =   modulePath.replace(moduleName+"."+componentExtension,"")+"bdp"+File.separator+moduleName;
			//pathBModuleInBdpFolderWithExtension =   modulePath+moduleName;		
		} catch (Exception e) {

			System.err.println("Error: you need specify the parameters");
		}

	}
	void setIsWD(boolean wd){
		isWD = wd;
		if(isWD){
			pathBModuleInBdpFolderWithoutExtension = pathBModuleInBdpFolderWithoutExtension.replace("_wd","")+"_wd";
			expressionsToEvaluate = new POs((pathBModuleInBdpFolderWithoutExtension.replace("_wd","")+"_wd"));
			
		}else{
			pathBModuleInBdpFolderWithoutExtension = pathBModuleInBdpFolderWithoutExtension.replace("_wd","");
			expressionsToEvaluate = new POs((pathBModuleInBdpFolderWithoutExtension.replace("_wd","")));
			
		}
	}

	void addRuleInPMMFile(boolean poTypedWD) {
		String fileNameToAddRule;
		final String finalFile = new String("\nEND");

		// if WD add WD else the normal name
		if (poTypedWD)
			fileNameToAddRule = new String(modulePath.substring(0,
					modulePath.length() - 4)
					+ "_wd.pmm");
		else
			fileNameToAddRule = new String(modulePath.substring(0,
					modulePath.length() - 3)
					+ "pmm");

		String fulltext = readFile(fileNameToAddRule);

		String[] theoriesSplitted = fulltext.split("END");

		// count the number of theories
		int numberTheories;
		if (theoriesSplitted[theoriesSplitted.length - 1].contains("THEORY"))
			numberTheories = theoriesSplitted.length;
		else
			numberTheories = theoriesSplitted.length - 1;

		StringBuffer stringInitialFile = new StringBuffer();
		for (int i = 0; i < numberTheories; i++) {

			// if(i!=numberTheories-1){ // the last
			stringInitialFile.append(theoriesSplitted[i] + "END");
			/*
			 * } else{ stringB.append(theoriesSplitted[i]); }
			 */
		}

		// if there is just one rule so add one ";" before the new rule
		String separator;
		if (theoriesSplitted[numberTheories - 1].contains("THEORY")) {
			separator = new String("\n&\n");
		}
		// else if(theoriesSplitted[numberTheories-1].contains("==") ) separator
		// = ";";
		else
			separator = "";

		String addedRule = new String(separator + "THEORY RulesProB"
				+ expressionName.replace(".", "_") + " IS \n\n"
				+ "\n\t /* Expression from " + expressionName
				+ ", it was added  in " + new Date()
				+ "\n\t  verified with ProB in " + total_time + " milliseconds"
				+ "\n\t  Module Path:" + modulePath + " */" + "\n\n\t " + goal
				+ "==btrue\n");

		String newRules = stringInitialFile + addedRule + finalFile;

		writeFile(fileNameToAddRule, newRules);
		// close the END
	}

	public static void writeFile(String pathName, String content) {
		PrintWriter pwr;
		try {

			pwr = new PrintWriter(new FileWriter(pathName));
			pwr.print(content);
			pwr.close();

		} catch (IOException e) {
			System.err.println("Error: it is not possible create the file:"
					+ pathName);
			e.printStackTrace();
		}

	}

	public static String readFile(String pathName) {

		StringBuffer res = new StringBuffer();
		String str;

		try {
			File fileDir = new File(pathName);

			BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(fileDir), "UTF8"));

			while ((str = in.readLine()) != null) {
				res.append(str + "\n");
			}

			in.close();
		} catch (UnsupportedEncodingException e) {
			System.out.println("Unsupported Encoding " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IOException " + e.getMessage());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		return res.toString();
	}

	public int callProbLogicEvaluator(boolean poTypedWD, boolean addRule,
			String parameters, String goalExpression) {

		exitVal = 0;

		try {
			Runtime rt = Runtime.getRuntime();

			String tmpPath = System.getProperty("java.io.tmpdir");
			String tmpFileName = tmpPath + File.separator + moduleName + "_"
					+ expressionName + ".goal";

			writeFile(tmpFileName, goalExpression);

			long initial_time = System.currentTimeMillis();
			
			//JOptionPane.showMessageDialog(null, this.getExecutablePath() + " "+ parameters.replace("\n", "") + " --eval_rule_file "+ tmpFileName);
			
			Process proc = rt.exec(this.getExecutablePath() + " "
					+ parameters.replace("\n", "") + " --eval_rule_file "
					+ tmpFileName);

			// any error message?
			StreamGobbler errorGobbler = new StreamGobbler(
					proc.getErrorStream(), "ERROR");

			// any output?
			StreamGobbler outputGobbler = new StreamGobbler(
					proc.getInputStream(), "OUTPUT"); // Can add a new parameter , System.out

			// kick them off
			errorGobbler.start();
			outputGobbler.start();

			exitVal = proc.waitFor();
			// Final time
			total_time = System.currentTimeMillis() - initial_time;

			Result res_out, res_error;
			res_out = outputGobbler.getResult();
			res_error = errorGobbler.getResult();

			if (res_out != Result.ERROR && res_error != Result.ERROR) {
				printSuccessFullyMsg();
				result = res_out;

				if (result == Result.TRUE && addRule) {
					addRuleInPMMFile(poTypedWD);
				}

			} else {
				result = res_error;
				exitVal = 1;
			}

			System.out.println("Time spent: " + total_time);
			System.out.println("Process exit value: " + exitVal);

		} catch (Throwable t) {
			t.printStackTrace();
		}
		return exitVal;

	}

	/**
	 * This method was build to support evaluation of a set of proof obligations
	 * 
	 * @param pathProBcli - It contains the path of Probcli
	 * @param parameters - It contains the parameters to call Probcli
	 * @param isFullProofObligation - When true the proof obligations is full, otherwise, the proof obligations has only the goal  
	 * @param report - It creates the spreadsheet
	 * @param fileNameOut - It contains the filename out
	 * @return
	 */
	public  Report callProbLogicEvaluatorModule(JFrame frame, JTextArea jTextArea , String pathProBcli,
			String parameters,  boolean isFullProofObligation,
			Report report, String fileNameOut) {

		int localExitVal = 0;
		long localTotalTime;
		String proofObligation;
		Result resultIndividual = Result.ERROR;
		
		//TODO: It is not needed the use the pathBModule
		POs expressionsToEvaluate = new POs((pathBModuleInBdpFolderWithoutExtension));
		
		//POs expressionsToEvaluate = new POs((pathBModuleInBdpFolderWithExtension.substring(0,
		//		pathBModuleInBdpFolderWithExtension.length() - 3) + "po"));
		
		int numberOftotalPOs = expressionsToEvaluate
				.getNumberOfProofObligations();

		StringBuffer proofObligations = new StringBuffer();
		try {
			for (int numberPo = 1; numberPo <= numberOftotalPOs; numberPo++) {
				Runtime rt = Runtime.getRuntime();

				String tmpPath = System.getProperty("java.io.tmpdir");
				String tmpFileName = tmpPath + File.separator + "po_"
						+ System.currentTimeMillis() + ".expanded.PO";

				if (isFullProofObligation) {
					proofObligation = expressionsToEvaluate
							.getCleanExpandedProofObligations(numberPo);
					writeFile(tmpFileName,proofObligation							);
				} else {
					proofObligation = expressionsToEvaluate
							//.getGoalOfCleanExpandedProofObligations(numberPo);
							.getCleanProofObligationsWithLocalHypotheses(numberPo);
					writeFile(tmpFileName,proofObligation	);
				}
				System.out.println(proofObligation);

				long initial_time = System.currentTimeMillis();

				Process proc = rt.exec(pathProBcli + " "
						+ parameters.replace("\n", "") + " --eval_rule_file "
						+ tmpFileName);
				
				
				/*
				// any error message?
				StreamGobbler errorGobbler = new StreamGobbler(	proc.getErrorStream(), "ERROR");

				// any output?
				StreamGobbler outputGobbler = new StreamGobbler(  proc.getInputStream(), "OUTPUT"); // Can add a new parameter , System.out
				
				
				
				// kick them off
				errorGobbler.start();
				outputGobbler.start();
				*/
				
				
				
				localExitVal = proc.waitFor();
				
				//printStream.flush();
				//jTextArea.repaint();
				//jTextArea.getGraphics().
				
				//outputGobbler.os. jTextArea();
				
				// Final time
				localTotalTime = System.currentTimeMillis() - initial_time;

				Result res_out, res_error;
				//res_out = outputGobbler.getResult();
				//res_error = errorGobbler.getResult();

				//res_out != Result.ERROR && res_error != Result.ERROR) {
				System.out.println("It was concluded successfully!");
				//resultIndividual = res_out;
				
				frame.repaint();
				//JOptionPane.showMessageDialog(null, numberPo);
				
				System.out.println("Time spent: " + localTotalTime);

				System.out.println("Process exit value: " + localExitVal);
				
				report.add(numberPo,
						parameters,
						POWD.Common,
						PoGenerated.Full,
						expressionsToEvaluate.getProofState(numberPo),
						proofObligation,
						Result.INITIAL,
						resultIndividual,
						localTotalTime);

				proofObligations.append(proofObligation + ";\n\n");

			}
			
			Control.writeFile(
					pathBModuleInBdpFolderWithoutExtension+ "full.EPOs", proofObligations.toString());
			

		} catch (Throwable t) {
			t.printStackTrace();
		}
		return report;

	}

	void printSuccessFullyMsg() {
		System.out.println("It was concluded successfully!");
		System.out.println("Prob checked and added the rule in:");

	}

	public Result getResult() {
		return result;
	}

	public String getCurrentPath() {
		return currentPath;
	}

	public StringBuffer getCommand() {
		return command;
	}

	public String getExpressionName() {
		return expressionName;
	}

	public String getModuleName() {
		return moduleName;
	}

	public String getHypothesis() {
		return hypothesis;
	}

	public String getModulePath() {
		return modulePath;
	}

	public String getSeparetor() {
		return separetor;
	}

	public String getComponentExtension() {
		return componentExtension;
	}

	public String getGoal() {
		return goal;
	}

	public long getTotal_time() {
		return total_time;
	}

	public int getIndExpressionName() {
		return indExpressionName;
	}

	public int getIndModuleName() {
		return indModuleName;
	}

	public int getIndModulePath() {
		return indModulePath;
	}

	public int getIndComponentExtension() {
		return indComponentExtension;
	}

	public int getIndGoal() {
		return indGoal;
	}

	public int getExitVal() {
		return exitVal;
	}
	public int getNumberOfProofObligations(){
		return expressionsToEvaluate.getNumberOfProofObligations();
	}

	public String[] getStateAndNameOfProofObligations(boolean isWD) {
		
		if(isWD){
			expressionsToEvaluate = new POs((pathBModuleInBdpFolderWithoutExtension+"_wd"));
		} else{
			expressionsToEvaluate = new POs((pathBModuleInBdpFolderWithoutExtension));
		}
		return expressionsToEvaluate.getStateAndNameOfProofObligations();
	}

	/**
	 * Returns true, when the proof obligation is evaluated true
	 * @param number enumerating from 1 up to numbers of proof obligations
	 * @return
	 */
	public boolean isProvedTheProofState(int number){
		
		return expressionsToEvaluate.isProvedTheProofState(number);
		
	}
	/**
	 * @param number enumerating from 1 up to numbers of proof obligations
	 * @return
	 */
	
	public String getCleanProofObligationsWithLocalHypotheses( int numberOfProofObligation){
		return expressionsToEvaluate.getCleanProofObligationsWithLocalHypotheses(numberOfProofObligation);
	}
	
	/***
	 * This method return only the goal, in other words, one proof obligation without comments and without hypothesis
	 * @param numberOfProofObligation  enumerating from 1 up to numbers of proof obligations
	 * @return
	 */
	public String getGoalOfCleanExpandedProofObligations(int numberOfProofObligation){
		
		return expressionsToEvaluate.getGoalOfCleanExpandedProofObligations(numberOfProofObligation);
	}

}
