//package br.com.fava.send.email;

package br.com.sankhya.fava;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.Report;
import br.com.sankhya.modelcore.util.ReportManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;

public class SendEmail implements AcaoRotinaJava {

	public void doAction(ContextoAcao ca) throws Exception {
		for (int i = 0; i < ca.getLinhas().length; i++) {
			Registro line = ca.getLinhas()[i];
			
			try {
				sendEmailCot(ca, line);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}

	private void sendEmailCot(ContextoAcao ca, Registro line) {

		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();

		BigDecimal nuAnexoPedido = BigDecimal.ZERO;
		BigDecimal ultCod = BigDecimal.ZERO;
		String email2 = null;
//		BigDecimal nuAnexoPedido2 = BigDecimal.ZERO;
//		BigDecimal ultCod2 = BigDecimal.ZERO;		
//		BigDecimal nuAnexoPedido3 = BigDecimal.ZERO;
//		BigDecimal ultCod3 = BigDecimal.ZERO;		
//		BigDecimal nuAnexoPedido4 = BigDecimal.ZERO;
//		BigDecimal ultCod4 = BigDecimal.ZERO;				
//		BigDecimal nuAnexoPedido5 = BigDecimal.ZERO;
//		BigDecimal ultCod5 = BigDecimal.ZERO;				
		BigDecimal nuNota = (BigDecimal) line.getCampo("NUNOTA");
		BigDecimal codParc = (BigDecimal) line.getCampo("CODPARC");
		BigDecimal top = (BigDecimal) line.getCampo("CODTIPOPER");
		String codEmp = (String) ca.getParam("P_CODEMP");
		String endereco = (String) ca.getParam("P_ENDERECO");
		String endereco2 = (String) ca.getParam("P_ENDERECO02");
		String endereco3 = (String) ca.getParam("P_ENDERECO03");
		String endereco4 = (String) ca.getParam("P_ENDERECO04");
		String endereco5 = (String) ca.getParam("P_ENDERECO05");
		
		Date dataAtual = new Date(System.currentTimeMillis());
		
		System.out.println("THIAGO BONATTI - INICIO DE TUDO" );	

		String msgRetorn = "Não foi possível enviar o e-mail. Verifique a aba contatos no cadastro do parceiro." + codParc;
		SessionHandle hnd = null;

		try {
			
			if (top.floatValue() == 7 || top.floatValue() == 23 || top.floatValue() == 83) {
				
				System.out.println("THIAGO BONATTI - ENTROU NA TOP" );	

				hnd = JapeSession.open();
				jdbc.openSession();
	
				// Pedido
				BigDecimal nroRelatorio = new BigDecimal(132);
				Map<String, Object> parameters = new HashMap<String, Object>();
				Report modeloImpressao = null;
				JasperPrint jasperPrint = null;
				parameters.put("NUNOTA", nuNota);
				modeloImpressao = ReportManager.getInstance().getReport(nroRelatorio, dwfEntityFacade);
				jasperPrint = modeloImpressao.buildJasperPrint(parameters, jdbc.getConnection());
				byte[] pedido = JasperExportManager.exportReportToPdf(jasperPrint);
	
				StringBuffer sqlTable = new StringBuffer();
				NativeSql nativeSql = new NativeSql(jdbc);
				ResultSet result = null;
	
				StringBuffer sqlEnd = new StringBuffer();
				NativeSql nativeEnd = new NativeSql(jdbc);
				ResultSet resultEnd = null;
				
				if (endereco == null || endereco.equals("")) {
					
					sqlEnd.append("SELECT    INITCAP(EN.NOMEEND) " + 
							"       || ', ' " + 
							"       || EMP.NUMEND " + 
							"       || ' ' " + 
							"       || INITCAP(BAI.NOMEBAI) " + 
							"       || ' - ' " + 
							"       || INITCAP(CID.NOMECID) " + 
							"       || ' ' " + 
							"       || UFS.UF " + 
							"          AS ENDERECO " + 
							"  FROM TSIEMP EMP, " + 
							"       TSIEND EN, " + 
							"       TSIBAI BAI, " + 
							"       TSICID CID, " + 
							"       TSIUFS UFS " + 
							" WHERE     EMP.CODEMP = " + codEmp  + 
							"       AND EMP.CODEND = EN.CODEND " + 
							"       AND EMP.CODBAI = BAI.CODBAI " + 
							"       AND EMP.CODCID = CID.CODCID " + 
							"       AND CID.UF = UFS.CODUF");
		
					resultEnd = nativeEnd.executeQuery(sqlEnd);
		
					while (resultEnd.next()) {
						endereco = resultEnd.getString("ENDERECO");
					}					
				}
		
				// Geração do e-mail
				String corpoemail = "<html> " + "<head> "
						+ "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\" /> "
						+ "  <title>Fatura</title> " + "</head> " + "<body> " + "  <p>Bom dia,</p> "
						+ "  <p>Segue pedido a ser despachado para " + endereco + ".</p> "
						+ "  <p>Favor informar na Nota Fiscal o número do pedido e os dados para pagamento em caso de deposito bancário.</p> "
						+ "  <p ><span><img border=\"0\" width=\"192\" height=\"100\" src=\"https://www.favasementes.com.br/site/images/logo-1.png\"></span><span></span></p> "
						+ "</body> " + "</html> ";
				char[] mensagem = corpoemail.toCharArray();
	
				sqlTable.append("SELECT NVL(CTT.EMAIL, 'error') AS EMAIL, SYSDATE AS DATA FROM TGFPAR PAR, TGFCTT CTT WHERE PAR.CODPARC = "
						+ codParc + " AND PAR.CODPARC = CTT.CODPARC");
	
				result = nativeSql.executeQuery(sqlTable);
	
				while (result.next()) {
					
					String email = result.getString("EMAIL");
					//Date dataAtual = result.getDate("DATA");
					
					if (!email.equals("error")) {
						
						// Insere o email para fila de envio
						try {												            

							if (email != null ) {
								
							System.out.println("THIAGO BONATTI - PRIMEIRO EMAIL" );	
	
							EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
							EntityVO entityVO = dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
							DynamicVO dynamicVO = (DynamicVO) entityVO;
							dynamicVO.setProperty("ASSUNTO", "Pedido de Compra");
							dynamicVO.setProperty("DTENTRADA", dataAtual);
							dynamicVO.setProperty("STATUS", "Pendente");
							dynamicVO.setProperty("EMAIL", email);
							dynamicVO.setProperty("TENTENVIO", new BigDecimal(1));
							dynamicVO.setProperty("MENSAGEM", mensagem);
							dynamicVO.setProperty("NUCHAVE", nuNota);
							dynamicVO.setProperty("TIPOENVIO", "E");
							dynamicVO.setProperty("MAXTENTENVIO", new BigDecimal(3));
							dynamicVO.setProperty("CODCON", new BigDecimal(0));
							PersistentLocalEntity createEntity = dwfFacade.createEntity("MSDFilaMensagem", entityVO);
							DynamicVO save = (DynamicVO) createEntity.getValueObject();
							ultCod = save.asBigDecimal("CODFILA");
	
							// Cria anexo pedido
							entityVO = dwfFacade.getDefaultValueObjectInstance("AnexoMensagem");
							dynamicVO = (DynamicVO) entityVO;
							dynamicVO.setProperty("NOMEARQUIVO", "Pedido.pdf");
							dynamicVO.setProperty("TIPO", "application/pdf");
							dynamicVO.setProperty("ANEXO", pedido);
							createEntity = dwfFacade.createEntity("AnexoMensagem", entityVO);
							save = (DynamicVO) createEntity.getValueObject();
							nuAnexoPedido = save.asBigDecimal("NUANEXO");
	
							// Insere o anexo da pedido
							nativeSql.executeUpdate(" INSERT INTO TMDAXM (CODFILA, NUANEXO) VALUES " + "(" + ultCod + " , "
									+ nuAnexoPedido + ")");
							
							msgRetorn = "Email inserido na fila de envio.";
							
							}

							
	
						} catch (Exception e) {
							e.printStackTrace();
							ca.setMensagemRetorno(e.getMessage());
	
						}
						
												
	
					}
	
				}
				
				int contador = 0;
			       
				while (contador < 4) {
		            System.out.println("Repetição nr: " + contador);
		            
		            if (contador == 0) {
					email2 = endereco2;
					}
					else if (contador == 1) {
					email2 = endereco3;
					}
					else if (contador == 2) {
					email2 = endereco4;
					}
					else if (contador == 3) {
					email2 = endereco5;
					}
		            
		            contador++;
		            try {												            

						if (email2 != null ) {

						EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
						EntityVO entityVO = dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
						DynamicVO dynamicVO = (DynamicVO) entityVO;
						dynamicVO.setProperty("ASSUNTO", "Pedido de Compra");
						dynamicVO.setProperty("DTENTRADA", dataAtual);
						dynamicVO.setProperty("STATUS", "Pendente");
						dynamicVO.setProperty("EMAIL", email2);
						dynamicVO.setProperty("TENTENVIO", new BigDecimal(1));
						dynamicVO.setProperty("MENSAGEM", mensagem);
						dynamicVO.setProperty("NUCHAVE", nuNota);
						dynamicVO.setProperty("TIPOENVIO", "E");
						dynamicVO.setProperty("MAXTENTENVIO", new BigDecimal(3));
						dynamicVO.setProperty("CODCON", new BigDecimal(0));
						PersistentLocalEntity createEntity = dwfFacade.createEntity("MSDFilaMensagem", entityVO);
						DynamicVO save = (DynamicVO) createEntity.getValueObject();
						ultCod = save.asBigDecimal("CODFILA");

						// Cria anexo pedido
						entityVO = dwfFacade.getDefaultValueObjectInstance("AnexoMensagem");
						dynamicVO = (DynamicVO) entityVO;
						dynamicVO.setProperty("NOMEARQUIVO", "Pedido.pdf");
						dynamicVO.setProperty("TIPO", "application/pdf");
						dynamicVO.setProperty("ANEXO", pedido);
						createEntity = dwfFacade.createEntity("AnexoMensagem", entityVO);
						save = (DynamicVO) createEntity.getValueObject();
						nuAnexoPedido = save.asBigDecimal("NUANEXO");

						// Insere o anexo da pedido
						nativeSql.executeUpdate(" INSERT INTO TMDAXM (CODFILA, NUANEXO) VALUES " + "(" + ultCod + " , "
								+ nuAnexoPedido + ")");
						
						msgRetorn = "Email inserido na fila de envio.";
						
						} 
						
		            } catch (Exception e) {
		    			e.printStackTrace();
		    			ca.setMensagemRetorno(e.getMessage());

				}
				
				}
				
			} else {
				msgRetorn = "Tipo de operação não suportado.";
			}

		} catch (Exception e) {
			e.printStackTrace();
			ca.setMensagemRetorno(e.getMessage());

		} 
	
		finally {
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
		}

		ca.setMensagemRetorno(msgRetorn);
	}
}