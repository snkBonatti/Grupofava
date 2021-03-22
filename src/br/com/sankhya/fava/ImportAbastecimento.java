package br.com.sankhya.fava;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.wrapper.JapeFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.StringUtils;
import com.sankhya.util.TimeUtils;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.vo.PrePersistEntityState;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.BarramentoRegra;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.ListenerParameters;
import br.com.sankhya.ws.ServiceContext;

public class ImportAbastecimento implements ScheduledAction {
	

	private AuthenticationInfo oldAuthInfo;
	private AuthenticationInfo authInfo;
	private final ServiceContext sctx = new ServiceContext(null);
	

	public void onTime(ScheduledActionContext arg0) {

		JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
		NativeSql nativeSql = new NativeSql(jdbc);

		StringBuilder sql = new StringBuilder();

		sql.append("SELECT MAX(NVL(LOTEIMP,0)) + 1 AS LOTE ");
		sql.append(" FROM AD_IMPABAST ");

		try {

			jdbc.openSession();
			ResultSet rs;

			rs = nativeSql.executeQuery(sql.toString());

			while (rs.next()) {

				BigDecimal lote = rs.getBigDecimal("LOTE");

				StringBuffer uriBuf = new StringBuffer(
						"" + "https://rest.scaplus.com.br/scaplus/rest/movimentacao/abastecimentos/get");

				System.out.println(uriBuf);

				URL url;
				try {
					url = new URL(uriBuf.toString());

					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setUseCaches(false);
					conn.setDoOutput(false);
					conn.setRequestMethod("GET");
					conn.setRequestProperty("charset", "utf-8");
					conn.setRequestProperty("token", "7107b5a2-34ae-11eb-adc1-0242ac120002");

					@SuppressWarnings("unused")
					int statusHttp = conn.getResponseCode();

					// System.out.println("statusHttp " + statusHttp);

					StringBuilder json = new StringBuilder("{'data': " + retornoJson(conn.getInputStream()) + "}");

					// System.out.println(json);

					JSONObject obj = null;

					try {

						obj = new JSONObject(json.toString());
						// System.out.println(obj.toString());
						JSONArray jArray = obj.getJSONArray("data");

						for (int i = 0; i < jArray.length(); i++) {

							JSONObject abast = jArray.getJSONObject(i);
							// System.out.println(abast.toString());

							Abastecimento abastecimento = (Abastecimento) new Gson().fromJson(abast.toString(),
									Abastecimento.class);

							System.out.println("thiago bonatti PARTE 01 - LOTE: " + lote);

							impAbastecimento(abastecimento, lote);

						}

					} catch (JSONException e) {
						e.printStackTrace();
					}

				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				jdbc.closeSession();

			}

		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
	}

	private String retornoJson(InputStream inputStream) throws UnsupportedEncodingException, IOException {

		StringBuffer response = new StringBuffer();
		byte[] buf = new byte['?'];
		int length;
		while ((length = inputStream.read(buf)) > 0) {
			response.append(new String(buf, 0, length, "utf-8"));
		}

		return response.toString();
	}

	private void impAbastecimento(Abastecimento abastecimento, BigDecimal lote) {

		SessionHandle hnd = null;

		/*
		 * hnd = JapeSession.open(); EntityFacade dwfFacade =
		 * EntityFacadeFactory.getDWFFacade();
		 * 
		 * 
		 * @SuppressWarnings("unchecked") Collection<DynamicVO> movimentos =
		 * dwfFacade.findByDynamicFinderAsVO( new FinderWrapper("AD_IMPABAST",
		 * "MOVICODIGO = ? ", new Object[] {BigDecimalUtil.valueOf((String)
		 * abastecimento.getMovi_codigo())}));
		 * 
		 * System.out.println("thiago bonatti - teste 1010: " + movimentos.size());
		 * 
		 * if (movimentos.size() > 0) {
		 * System.out.println("thiago bonatti - teste 1020: " + movimentos.size());
		 * return; } else
		 * 
		 * {
		 */
		hnd = JapeSession.open();
		JapeWrapper impDAO = JapeFactory.dao("AD_IMPABAST");

		System.out.println("thiago bonatti - teste: " + lote);

		System.out.println("thiago bonatti - Hora inicio: " + abastecimento.getMovi_datahora_inicio());

		JdbcWrapper JDBC = JapeFactory.getEntityFacade().getJdbcWrapper();
		NativeSql nativeSql = new NativeSql(JDBC);

		StringBuilder sql = new StringBuilder();

		sql.append("SELECT COUNT(*)  AS COUNT " + " FROM AD_IMPABAST " + " WHERE MOVICODIGO = "
				+ BigDecimalUtil.valueOf((String) abastecimento.getMovi_codigo()));

		ResultSet rs;
		try {

			JDBC.openSession();

			rs = nativeSql.executeQuery(sql.toString());

			while (rs.next()) {

				BigDecimal count = rs.getBigDecimal("COUNT");
				System.out.println("thiago bonatti - teste 1010 COUNT: " + count);

				if (count.equals(BigDecimal.ZERO)) {
					try {

						@SuppressWarnings("unused")
						DynamicVO save = impDAO.create()
								.set("MOVICODIGO", BigDecimalUtil.valueOf((String) abastecimento.getMovi_codigo()))
								.set("POCO_CODIGO", abastecimento.getPoco_codigo())
								.set("POCO_DESCRICAO", abastecimento.getPoco_descricao())
								.set("POCO_CODIGO_AUX", abastecimento.getPoco_codigo_aux())
								.set("POCO_DEPOSITO", abastecimento.getPoco_deposito())
								.set("POCO_CENTRO", abastecimento.getPoco_centro())
								.set("POCO_CGC", abastecimento.getPoco_cgc())
								.set("TAFI_CODIGO", abastecimento.getTafi_codigo())
								.set("BICO_NUMERO", BigDecimalUtil.valueOf((String) abastecimento.getBico_numero()))
								.set("MOVI_HORA_INICIO2", abastecimento.getMovi_datahora_inicio())
								.set("MOVI_HORA_FIM2", abastecimento.getMovi_datahora_fim())
								// .set("MOVI_DURACAO", BigDecimalUtil.valueOf((String)
								// abastecimento.getMovi_duracao()))
								.set("EMPR_CODIGO", abastecimento.getEmpr_codigo())
								.set("EMPR_DESCRICAO", abastecimento.getEmpr_descricao())
								.set("EMPR_DESCRICAO_ABREVIADA", abastecimento.getEmpr_descricao_abreviada())
								.set("VEIC_CODIGOFROTA", abastecimento.getVeic_codigofrota())
								.set("VEIC_PLACA", abastecimento.getVeic_placa())
								.set("VEIC_TIPO", abastecimento.getVeic_tipo())
								.set("MOVI_ENCERRANTE_INICIAL",
										BigDecimalUtil.valueOf((String) abastecimento.getMovi_encerrante_inicial()))
								.set("MOVI_ENCERRANTE_FINAL",
										BigDecimalUtil.valueOf((String) abastecimento.getMovi_encerrante_final()))
								.set("MOVI_VOLUME", BigDecimalUtil.valueOf((String) abastecimento.getMovi_volume()))
								.set("MOVI_TOTALIZADOR_VEICULO",
										BigDecimalUtil.valueOf((String) abastecimento.getMovi_totalizador_veiculo()))
								.set("MOVI_TOTALIZADOR_VEIC_ANTERIOR",
										BigDecimalUtil
												.valueOf((String) abastecimento.getMovi_totalizador_veic_anterior()))
								.set("MOVI_TOTALIZADOR_RODADO",
										BigDecimalUtil.valueOf((String) abastecimento.getMovi_totalizador_rodado()))
								.set("VEIC_TIPO_TOTALIZADOR", abastecimento.getVeic_tipo_totalizador())
								.set("MOVI_DESEMPENHO",
										BigDecimalUtil.valueOf((String) abastecimento.getMovi_desempenho()))
								.set("MOVE_CONSUMO_MEDIO",
										BigDecimalUtil.valueOf((String) abastecimento.getMove_consumo_medio()))
								.set("MOVI_MODO", abastecimento.getMovi_modo())
								.set("MOVI_FORMA_ID", BigDecimalUtil.valueOf((String) abastecimento.getMovi_forma_id()))
								.set("USUA_LOGIN", abastecimento.getUsua_login())
								.set("USUA_NOME", abastecimento.getUsua_nome())
								.set("PROD_CODIGO", BigDecimalUtil.valueOf((String) abastecimento.getProd_codigo()))
								.set("PROD_DISPLAY", abastecimento.getProd_display())
								.set("PROD_CODIGO_AUX", abastecimento.getProd_codigo_aux())
								.set("PROD_TIPO", abastecimento.getProd_tipo())
								.set("CECU_CODIGO", abastecimento.getCecu_codigo())
								.set("CECU_DESCRICAO", abastecimento.getCecu_descricao())
								.set("OPER_CODIGO", abastecimento.getOper_codigo())
								.set("OPER_DESCRICAO", abastecimento.getOper_descricao())
								.set("MOVI_TIPO_MOVIMENTO",
										BigDecimalUtil.valueOf((String) abastecimento.getMovi_tipo_movimento()))
								.set("MOTO_CODIGO", abastecimento.getMoto_codigo())
								.set("MOTO_NOME", abastecimento.getMoto_nome())
								.set("VEMO_CODIGO", BigDecimalUtil.valueOf((String) abastecimento.getVemo_codigo()))
								.set("VEMO_DESCRICAO", abastecimento.getVemo_descricao())
								.set("VEMA_CODIGO", BigDecimalUtil.valueOf((String) abastecimento.getVema_codigo()))
								.set("VEMA_DESCRICAO", abastecimento.getVema_descricao())
								.set("MOVI_PRECO_UNITARIO",
										BigDecimalUtil.valueOf((String) abastecimento.getMovi_preco_unitario()))
								.set("MOVI_PRECO_TOTAL",
										BigDecimalUtil.valueOf((String) abastecimento.getMovi_preco_total()))
								.set("EMPR_CODIGO_AUXI", abastecimento.getEmpr_codigo_auxi())
								.set("MOVI_ONLINE", abastecimento.getMovi_online()).set("LOTEIMP", lote).save();

					} catch (NumberFormatException e) {
						e.printStackTrace();
						// return;
						JapeSession.close(hnd);
					}
				}
			}

			JDBC.closeSession();

		} catch (Exception e) {
			e.printStackTrace();
			JapeSession.close(hnd);
		} finally {
			JapeSession.close(hnd);
		}

		return;
		
	
	}
	
	protected void registry(BigDecimal codUsu) throws Exception {	

		oldAuthInfo = AuthenticationInfo.getCurrentOrNull();

		if (oldAuthInfo != null) {
			AuthenticationInfo.unregistry();
		}

		DynamicVO usuarioVO = (DynamicVO) EntityFacadeFactory.getDWFFacade()
				.findEntityByPrimaryKeyAsVO(DynamicEntityNames.USUARIO, new Object[] { codUsu });

		StringBuffer authID = new StringBuffer();
		authID.append(System.currentTimeMillis()).append(':').append(usuarioVO.asBigDecimal("CODUSU")).append(':')
				.append(this.hashCode());

		authInfo = new AuthenticationInfo(usuarioVO.asString("NOMEUSU"), usuarioVO.asBigDecimalOrZero("CODUSU"),
				usuarioVO.asBigDecimalOrZero("CODGRUPO"), new Integer(authID.toString().hashCode()));
		authInfo.makeCurrent();

		sctx.setAutentication(authInfo);
		sctx.makeCurrent();

		JapeSessionContext.putProperty("usuario_logado", authInfo.getUserID());
		JapeSessionContext.putProperty("emp_usu_logado", usuarioVO.asBigDecimal("CODEMP"));
		JapeSessionContext.putProperty("dh_atual", new Timestamp(System.currentTimeMillis()));
		JapeSessionContext.putProperty("d_atual", new Timestamp(TimeUtils.getToday()));
		JapeSessionContext.putProperty("usuarioVO", usuarioVO);
		JapeSessionContext.putProperty("authInfo", authInfo);

		// }
	}	

}
