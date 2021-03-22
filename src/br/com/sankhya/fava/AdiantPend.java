package br.com.sankhya.fava;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

//import org.jdom.Element;
//import br.com.sankhya.dwf.services.ServiceUtils;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
//import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.dwfdata.vo.tgf.FinanceiroVO;
import br.com.sankhya.modelcore.facades.BaixaFinanceiroSP;
import br.com.sankhya.modelcore.financeiro.helper.BaixaHelper;
import br.com.sankhya.modelcore.financeiro.util.AdiantamentoEmprestimoHelper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.jape.dao.EntityDAO;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.financeiro.util.AdiantamentoEmprestimoHelper.CobrancaJuros;
import br.com.sankhya.modelcore.financeiro.util.DadosBaixa;
import br.com.sankhya.modelcore.financeiro.util.DadosParcelamento;
import br.com.sankhya.modelcore.financeiro.util.ParcelamentoEmprestimoHelper;


import com.sankhya.util.TimeUtils;

public class AdiantPend implements AcaoRotinaJava {

		@Override
		public void doAction(ContextoAcao ctx) throws Exception {

			for (int i = 0; i < ctx.getLinhas().length; i++) {
				Registro line = ctx.getLinhas()[i];
				try {
					
					exemplo(ctx, line);
				
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}	
		
		@SuppressWarnings("static-access")	
		private void exemplo(ContextoAcao ctx, Registro line) {
			
			AuthenticationInfo authInfo = AuthenticationInfo.getCurrent();
			JapeSession.SessionHandle hnd = null;
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			
			BigDecimal nunota = (BigDecimal) line.getCampo("NUNOTA");
			BigDecimal codParc = (BigDecimal) line.getCampo("CODPARC");
			BigDecimal vlrNota = (BigDecimal) line.getCampo("VLRNOTA");
			String tipMov = line.getCampo("TIPMOV").toString();
			BigDecimal adiantNota = (BigDecimal) line.getCampo("AD_NUADIANTORIG");
			BigDecimal codctabcoint = new BigDecimal(110);
			BigDecimal codlanc = new BigDecimal(2);
			BigDecimal codlancRec = new BigDecimal(1);
			
			QueryExecutor query = ctx.getQuery();
			QueryExecutor query2 = ctx.getQuery();
			QueryExecutor query3 = ctx.getQuery();
			QueryExecutor query4 = ctx.getQuery();
			
	        StringBuilder sql = new StringBuilder();
	        
	        sql.append("SELECT MAX(CAB.CODPARC) AS CODPARC ");
	        sql.append("FROM TGFCAB CAB, TGFVAR VAR ");
	        sql.append("WHERE CAB.NUNOTA = VAR.NUNOTAORIG ");
	        sql.append("AND VAR.NUNOTA = " + nunota );
	        
	        try {
	        query.nativeSelect(sql.toString());
	        
	        while (query.next()) {
	        	
	        	BigDecimal codParcOrig = query.getBigDecimal("CODPARC");
	        	
	        	System.out.println("CODIGO DE PARCEIRO " + query.getBigDecimal("CODPARC"));
	        	
	        	// se parceiro for diferente 
	        	if (codParcOrig.compareTo(codParc) != 0 & adiantNota != null) {
	        		
	        		ctx.setMensagemRetorno("Procedimento já havia sido executado!");
	        	}
	        	if (codParcOrig.compareTo(codParc) != 0 & adiantNota == null) {
	        		
	    	        StringBuilder sql2 = new StringBuilder();
	    	        
	    	        sql2.append("SELECT MAX(NUFIN) AS NUFIN, MAX(NUMDUPL) AS NUMDUPL"); 
	    	        sql2.append("  FROM TGFFIN "); 
	    	        sql2.append(" WHERE CODPARC = " + codParcOrig ); 
	    	        sql2.append("  AND RECDESP = -1  "); 
	    	        sql2.append("  AND DHBAIXA IS NULL ");  
	    	        sql2.append("  AND NUMDUPL IS NOT NULL " );  
	    	        sql2.append("  AND DESDOBDUPL LIKE 'ZZ' ");   
	    	        sql2.append("  AND VLRDESDOB >= " + vlrNota); 
	    	        
	    	        query2.nativeSelect(sql2.toString());
	    	        
	    	        System.out.println("PONTO 1 TESTE - THIAGOBONATTI ADIANT NOTA: "+ adiantNota);
	    	        
	    	        
		    	    // se tiver financeiro pendente    
	    	        while (query2.next()) {
		    	        	
		    	        	BigDecimal nufin = query2.getBigDecimal("NUFIN");	
		    	        	BigDecimal nuadiantOrig = query2.getBigDecimal("NUMDUPL");	
		    	        	
			    	        System.out.println("PONTO 2 TESTE - THIAGOBONATTI NUFIN ADIANT: "+ nufin);
			    			
			    	        StringBuilder sql3 = new StringBuilder();
			    	        
			    	        sql3.append("  SELECT CODEMP, CODTIPTIT, 33 AS CODTIPTITDESP, CODTIPOPER, DTVENC, DTNEG, CODNAT, CODCENCUS, CODPROJ, HISTORICO, CODCTABCOINT, VLRDESDOB "); 
			    	        sql3.append("  FROM TGFFIN WHERE NUFIN = " + nufin ); 
			    	        
			    	        query3.nativeSelect(sql3.toString());
		    	        	
			    	        while (query3.next()) {
			    	        	
			    	        BigDecimal codemp = query3.getBigDecimal("CODEMP");	
			    	        BigDecimal codtipoper = query3.getBigDecimal("CODTIPOPER");	
			    	        int codctabcointRec = query3.getInt("CODCTABCOINT");	
			    	        BigDecimal codctabcointRecBig = query3.getBigDecimal("CODCTABCOINT");
			    	        BigDecimal codtiptit = query3.getBigDecimal("CODTIPTIT");	
			    	        BigDecimal codtiptitDesp = query3.getBigDecimal("CODTIPTITDESP");
			    	        BigDecimal codnat = query3.getBigDecimal("CODNAT");	
			    	        BigDecimal codcencus = query3.getBigDecimal("CODCENCUS");
			    	        BigDecimal codproj = query3.getBigDecimal("CODPROJ");
			    	        BigDecimal vlrDesdob = query3.getBigDecimal("VLRDESDOB");
			    	        Date dtneg = query3.getDate("DTNEG");
			    	        
			    	        Date dtvenc = query3.getDate("DTVENC");
			    	        String historico = " Adiantamento ";
			    	        //Timestamp dtvencTime = (Timestamp) dtvenc;
			    	        Timestamp dtvencTime = query3.getTimestamp("DTVENC");
			    	        Timestamp dtnegTime = query3.getTimestamp("DTNEG");
			    	        
			    	        BigDecimal vlrPendencia = vlrDesdob.subtract(vlrNota);
			    	        
		    	        	// baixar titulo
		    	        	
			    	        System.out.println("PONTO 3 TESTE - THIAGOBONATTI ANTES DE BAIXAR CODEMP "+ codemp);
			    	        
			    	        	BigDecimal codUsuarioLogado = AuthenticationInfo.getCurrent().getUserID();

			    	        	//DynamicVO tipTitVO = finVO.asDymamicVO("TipoTitulo");

			    	            //validarDadosDeBaixa(config, tipTitVO);
			    	            
			    	            BaixaHelper baixaHelper = new BaixaHelper(nufin, codUsuarioLogado);
			    	            
			    	            Timestamp dtBaixa = new Timestamp(TimeUtils.getToday());
			    	            DadosBaixa dadosBaixa = new DadosBaixa(dtBaixa);
			    	            
			    	            DadosBaixa.ValoresBaixa valoresBaixa = dadosBaixa.getValoresBaixa();
			    	            
			    	            dadosBaixa.getDadosAdicionais().setCodEmpresa(codemp);
			    	            dadosBaixa.getDadosBancarios().setCodConta(codctabcoint);
			    	            dadosBaixa.getDadosBancarios().setCodLancamento(codlanc);
			    	            dadosBaixa.getDadosAdicionais().setCodTipoOperacao(codtipoper);
			    	            //dadosBaixa.getValoresBaixa().setVlrTotal(vlrNota.doubleValue());
			    	            //dadosBaixa.getDadosBancarios().setNumDocumento(finVO.asBigDecimal("NUMNOTA"));
			    	            //dadosBaixa.getValoresBaixa().setVlrDesconto(finVO.asDouble("VLRDESC"));
			    	            //dadosBaixa.getValoresBaixa().setTaxaAdm(finVO.asDouble("CARTAODESC"));
			    	            //dadosBaixa.getValoresBaixa().setVlrMulta(finVO.asDouble("VLRMULTA"));
			    	            //dadosBaixa.getValoresBaixa().setVlrJuros(finVO.asDouble("VLRJURO"));
			    	            
			    	            //valoresBaixa.setVlrBaixa(vlrNota.doubleValue());
			    	            valoresBaixa.setVlrTotal(vlrDesdob.doubleValue());
			    	            valoresBaixa.setVlrDesconto(0);
			    	            dadosBaixa.getDadosPendencia().setVlrTotal(vlrPendencia.doubleValue());
			    	            dadosBaixa.getDadosPendencia().setDtVencimento(dtvencTime);
			    	            dadosBaixa.getDescisaoBaixa().setDescisao(4);
			    	                      
			    	            dadosBaixa.getValoresBaixa().setVlrTotal(BaixaHelper.calculaValorBaixa(dadosBaixa, vlrNota.doubleValue() , dadosBaixa.getImpostos().getOutrosImpostos(), 0));

			    	            
			    	            System.out.println("PONTO 4 TESTE - THIAGOBONATTI ANTES DA BAIXA Pendencia: " + vlrPendencia + " Vlr Nota: " +  vlrNota + " Vlr Financeiro " + vlrDesdob ); 
			    	            
			    	            baixaHelper.baixar(dadosBaixa);
			    	           			    	        
		    	        	
		    	        	// incluir novo adiantamento
		    	        	
		    	        	// despesa
							DynamicVO despesaVO = (DynamicVO) dwfFacade.getDefaultValueObjectInstance("Financeiro",
									FinanceiroVO.class);

		    				despesaVO.setProperty("RECDESP", BigDecimal.ONE.negate());
		    				despesaVO.setProperty("CODEMP", codemp);
		    				despesaVO.setProperty("PROVISAO", "N");
		    				despesaVO.setProperty("CODTIPOPER", codtipoper);
		    				despesaVO.setProperty("CODPARC", codParc);
		    				despesaVO.setProperty("HISTORICO", historico);
		    				despesaVO.setProperty("CODCTABCOINT", codctabcointRecBig);
		    				despesaVO.setProperty("CODBCO", getCodigoBanco(codctabcointRec));
		    				despesaVO.setProperty("CODTIPTIT", codtiptitDesp);
		    				despesaVO.setProperty("CODNAT", codnat);
		    				despesaVO.setProperty("CODCENCUS", codcencus);
		    				despesaVO.setProperty("ORIGEM", "F");
		    				despesaVO.setProperty("CODPROJ", codproj);
		    				despesaVO.setProperty("DTNEG", dtnegTime);
		    				despesaVO.setProperty("DTVENC", dtvencTime);
		    				despesaVO.setProperty("DTVENCINIC", dtvencTime);
		    				despesaVO.setProperty("DESDOBRAMENTO", "0");
		    				despesaVO.setProperty("DESDOBDUPL", "ZZ");
		    				despesaVO.setProperty("TIPMARCCHEQ", "I");
		    				despesaVO.setProperty("TIPMULTA", "1");
		    				despesaVO.setProperty("TIPJURO", "1");
		    				despesaVO.setProperty("VLRDESDOB", vlrNota);
		    				
		    				
		    				
		    				System.out.println("PONTO 5 TESTE - THIAGOBONATTI DEPOIS DE INCLUIR DESPESA: "); 

		    				DynamicVO receitaVO = (DynamicVO) dwfFacade.getDefaultValueObjectInstance("Financeiro",
		    						FinanceiroVO.class);

		    				receitaVO.setProperty("RECDESP", BigDecimal.ONE);
		    				receitaVO.setProperty("CODEMP", codemp);
		    				receitaVO.setProperty("PROVISAO", "N");
		    				receitaVO.setProperty("CODTIPOPER", codtipoper);
		    				receitaVO.setProperty("CODPARC", codParc);
		    				receitaVO.setProperty("HISTORICO", historico);
		    				receitaVO.setProperty("CODCTABCOINT", codctabcoint);
		    				receitaVO.setProperty("CODBCO", new BigDecimal(999));
		    				receitaVO.setProperty("CODTIPTIT", codtiptit);
		    				receitaVO.setProperty("CODNAT", codnat);
		    				receitaVO.setProperty("CODCENCUS", codcencus);
		    				receitaVO.setProperty("ORIGEM", "F");
		    				receitaVO.setProperty("CODPROJ", codproj);
		    				receitaVO.setProperty("DTNEG", dtnegTime);
		    				receitaVO.setProperty("DTVENC", dtvencTime);
		    				receitaVO.setProperty("DTVENCINIC", dtvencTime);
		    				receitaVO.setProperty("DESDOBRAMENTO", "0");
		    				receitaVO.setProperty("DESDOBDUPL", "ZZ");
		    				receitaVO.setProperty("TIPMARCCHEQ", "I");
		    				receitaVO.setProperty("TIPMULTA", "1");
		    				receitaVO.setProperty("TIPJURO", "1");
		    				receitaVO.setProperty("VLRDESDOB", vlrNota);
		    				
		    				System.out.println("PONTO 6 TESTE - THIAGOBONATTI DEPOIS DE INCLUIR RECEITA: ");
		    				

		    				try {

		    					hnd = JapeSession.open();

		    					AdiantamentoEmprestimoHelper helper = new AdiantamentoEmprestimoHelper();

		    					Collection<DynamicVO> titulosParcelamento = new ArrayList();

		    					titulosParcelamento.add(despesaVO);
		    					titulosParcelamento.add(receitaVO);
		    					
		    					
		    					//BigDecimal nufinRec = (BigDecimal) receitaVO.getProperty("NUFIN");
		    					//BigDecimal nufinDesp = (BigDecimal) despesaVO.getProperty("NUFIN");
		    					
		    					System.out.println("PONTO 7 TESTE - THIAGOBONATTI nro unico receita: " + 0 + " nufin despesa: " + 0);
		    							
		    					BigDecimal numeroAcerto = helper.salvarParcelamento(titulosParcelamento, authInfo.getUserID());

		    					
		    					System.out.println("PONTO 8 TESTE - THIAGOBONATTI nro acerto: " + numeroAcerto );
			    				
			    				System.out.println("PONTO 9 TESTE - THIAGOBONATTI Adiantamento orig" + nuadiantOrig);
			    				
			    				line.setCampo("AD_NUADIANTDEST", numeroAcerto);
			    				line.setCampo("AD_NUADIANTORIG", nuadiantOrig);
			    				
			    				StringBuilder sql4 = new StringBuilder();
				    	        
				    	        sql4.append("  SELECT NUFIN ");
				    	        sql4.append("   FROM TGFFRE "); 
				    	        sql4.append("  WHERE SEQUENCIA = 2 AND NUACERTO = " + numeroAcerto ); 
				    	        
				    	        query4.nativeSelect(sql4.toString());
			    	        	
				    	        while (query4.next()) {
				    	        	
				    	        BigDecimal nufinBaixa = query4.getBigDecimal("NUFIN");	
				    	        
				    	        // baixa da receita
				    	        
			    	            BaixaHelper baixaHelper2 = new BaixaHelper(nufinBaixa, codUsuarioLogado);
			    	            
			    	            Timestamp dtBaixa2 = new Timestamp(TimeUtils.getToday());
			    	            DadosBaixa dadosBaixa2 = new DadosBaixa(dtBaixa2);
			    	            
			    	            DadosBaixa.ValoresBaixa valoresBaixa2 = dadosBaixa2.getValoresBaixa();
			    	            
			    	            dadosBaixa2.getDadosAdicionais().setCodEmpresa(codemp);
			    	            dadosBaixa2.getDadosBancarios().setCodConta(codctabcoint);
			    	            dadosBaixa2.getDadosBancarios().setCodLancamento(codlancRec);
			    	            dadosBaixa2.getDadosAdicionais().setCodTipoOperacao(codtipoper);
			    	            //dadosBaixa.getValoresBaixa().setVlrTotal(vlrNota.doubleValue());
			    	            //dadosBaixa.getDadosBancarios().setNumDocumento(finVO.asBigDecimal("NUMNOTA"));
			    	            //dadosBaixa.getValoresBaixa().setVlrDesconto(finVO.asDouble("VLRDESC"));
			    	            //dadosBaixa.getValoresBaixa().setTaxaAdm(finVO.asDouble("CARTAODESC"));
			    	            //dadosBaixa.getValoresBaixa().setVlrMulta(finVO.asDouble("VLRMULTA"));
			    	            //dadosBaixa.getValoresBaixa().setVlrJuros(finVO.asDouble("VLRJURO"));
			    	            
			    	            //valoresBaixa.setVlrBaixa(vlrNota.doubleValue());
			    	            valoresBaixa2.setVlrTotal(vlrNota.doubleValue());
			    	            valoresBaixa2.setVlrDesconto(0);
			    	            //dadosBaixa2.getDadosPendencia().setVlrTotal(vlrPendencia.doubleValue());
			    	            //dadosBaixa2.getDadosPendencia().setDtVencimento(dtvencTime);
			    	            //dadosBaixa2.getDescisaoBaixa().setDescisao(4);
			    	                      
			    	            dadosBaixa2.getValoresBaixa().setVlrTotal(BaixaHelper.calculaValorBaixa(dadosBaixa2, vlrNota.doubleValue() , dadosBaixa2.getImpostos().getOutrosImpostos(), 0));

			    	            
			    	            System.out.println("PONTO 10 TESTE - THIAGOBONATTI ANTES DA BAIXA Pendencia: " + vlrPendencia + " Vlr Nota: " +  vlrNota + " Vlr Financeiro " + vlrDesdob ); 
			    	            
			    	            baixaHelper2.baixar(dadosBaixa2);

				    	        	
				    	        	
				    	        }

			    	        	ctx.setMensagemRetorno("Procedimento executado com sucesso! Nro Adiantamento Novo: " + numeroAcerto );
			    	        	
		    				} catch (Exception e) {
		    					MGEModelException.throwMe(e);
		    				} finally {
		    					JapeSession.close(hnd);
		    				}
		    	        	
		    	        	// fim adiantamento
		    				

		    	        	
		    	        }
	    	        }
	        		
	        		//ctx.setMensagemRetorno("Rotina só pode ser usada se Parceiro Origem possuir adiantamento pendente! Parceiro Origem Diferente: " + codParcOrig + " Destino: " + codParc);
	        		
	        	} 
	        	// se parceiro for igual
	        	else {
	        		
	        		ctx.setMensagemRetorno("Rotina só pode ser usada se Parceiro Origem possuir adiantamento pendente! Parceiro Origem: " + codParcOrig + " Destino: " + codParc);
	        		
	        	}
	        	
	        	
	        	
			}
			
	        } catch (Exception e2) {
	        	
	        	query.close();
	        	
	        	query2.close();
	        	
	        	query3.close();
	        	
				e2.printStackTrace();

				System.out.println("Fim ---- thiago bonatti -- erro");
			} finally {
				
	        	query.close();
	        	
	        	query2.close();
	        	
	        	query3.close();
	        	
			}

		}
		
		private Object getCodigoBanco(int codctabcoint) throws Exception {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();

			DynamicVO contaVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("ContaBancaria", codctabcoint);

			return contaVO.asBigDecimal("CODBCO");
		}		

}
