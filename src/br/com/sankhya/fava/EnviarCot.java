package br.com.sankhya.fava;

import java.math.BigDecimal;
import java.sql.ResultSet;
import br.com.sankhya.cotacao.model.services.CotacaoHelper;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;
import org.jdom.Element;

import com.sankhya.util.XMLUtils;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class EnviarCot implements ScheduledAction {

	public void onTime(ScheduledActionContext arg0) {
		JdbcWrapper JDBC = JapeFactory.getEntityFacade().getJdbcWrapper();
		NativeSql nativeSql = new NativeSql(JDBC);

		StringBuilder sql = new StringBuilder();

		sql.append("SELECT C.NUMCOTACAO, I.CODLOCAL, I.CODPROD, I.CONTROLE, I.DIFERENCIADOR ,  PRO.DESCRPROD ");
		sql.append(
				"FROM AD_AGENDCOT C, TGFITC I, TGFPRO PRO "
				+ "WHERE C.NUMCOTACAO = I.NUMCOTACAO "
				+ "AND I.CODPROD = PRO.CODPROD "
				+ "AND C.STATUS = 'P' "
				+ "AND I.CABECALHO = 'N'");

		ResultSet rs;
		try {
			rs = nativeSql.executeQuery(sql.toString());

			while (rs.next()) {
				/*
				 * CotacaoHelper helper = new CotacaoHelper(dwfFacade, jdbc); String result =
				 * helper.enviarItensCotacao(ctx);
				 */

				SessionHandle hnd = null;
				JdbcWrapper jdbc = null;

				try {

					Element requestBody = new Element("requestBody");

					Element parametrosElem = new Element("parametros");
					requestBody.addContent(parametrosElem);

					Element preferenciasElem = new Element("preferenciasCotacao");
					preferenciasElem.setAttribute("numCotacao", rs.getString("NUMCOTACAO"));

					Element itensCotacaoElem = new Element("itensCotacao");
					Element itemCotacaoElem = new Element("itemCotacao");
					XMLUtils.addCDATAContentElement(itemCotacaoElem, "CODLOCAL", rs.getBigDecimal("CODLOCAL"));
					XMLUtils.addCDATAContentElement(itemCotacaoElem, "CODPROD", rs.getBigDecimal("CODPROD"));
					XMLUtils.addCDATAContentElement(itemCotacaoElem, "CONTROLE", rs.getString("CONTROLE"));
					XMLUtils.addCDATAContentElement(itemCotacaoElem, "DIFERENCIADOR", rs.getString("DIFERENCIADOR"));
					XMLUtils.addCDATAContentElement(itemCotacaoElem, "NUMCOTACAO", rs.getBigDecimal("NUMCOTACAO"));
					XMLUtils.addCDATAContentElement(itemCotacaoElem, "Produto_DESCRPROD", rs.getString("DESCRPROD"));

					parametrosElem.addContent(preferenciasElem);
					parametrosElem.addContent(itensCotacaoElem);
					itensCotacaoElem.addContent(itemCotacaoElem);

					ServiceContext ctx = ServiceContext.getCurrent();
					ctx.getBodyElement().setContent(requestBody);
					//ctx.getBodyElement().addContent(requestBody);
					// .setRequestBody(requestBody);

					EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
					hnd = JapeSession.open();
					jdbc = dwfFacade.getJdbcWrapper();
					jdbc.openSession();

					CotacaoHelper helper = new CotacaoHelper(dwfFacade, jdbc);
					String result = helper.enviarItensCotacao(ctx);

					nativeSql.executeUpdate("UPDATE AD_AGENDCOT SET STATUS = 'E', " 
							+ " ERRO = '" + result + "' WHERE NUMCOTACAO = " + rs.getBigDecimal("NUMCOTACAO"));

					System.out.println("thiagobonatti - resultado: " + result);

				} catch (Exception e) {

					e.printStackTrace();
					
				} finally {
					JapeSession.close(hnd);
				}

				System.out.println(" CATACAO ENVIADA: " + rs.getBigDecimal("NUMCOTACAO"));

			}

		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
	}

}
