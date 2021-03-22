package br.com.sankhya.fava;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.lowagie.text.pdf.codec.Base64.OutputStream;

import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.wrapper.JapeFactory;


public class ExportAbast implements ScheduledAction { 
	
	public void onTime(ScheduledActionContext arg0) {
		
		JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
        NativeSql nativeSql = new NativeSql(jdbc);
        
        StringBuilder sql = new StringBuilder();
        
        sql.append("SELECT MOVICODIGO AS MOVICODIGO ");
        sql.append(" FROM AD_IMPABAST ");
        sql.append(" WHERE NVL(RETORNO,'N') = 'N' ");
        
        String corpo = "INICIO";
        
		try {
			
			jdbc.openSession();
			ResultSet rs;
			
			rs = nativeSql.executeQuery(sql.toString());
			
			while (rs.next()) {
				
				System.out.println(" THIAGO BONATTI - CURSOR CODIGOS" + rs.getBigDecimal("MOVICODIGO"));
			
				//StringBuilder json = new StringBuilder("{'MOVI_CODIGO': " +  rs.getBigDecimal("MOVICODIGO")+ "}");
				String dados =  " "+ rs.getBigDecimal("MOVICODIGO") ;
				
				if (corpo != null) {
				corpo = corpo + ","  +  dados;
				}
				
			}
			
			jdbc.closeSession();

		
		StringBuffer uriBuf = new StringBuffer(""
				+ "https://rest.scaplus.com.br/scaplus/rest/movimentacao/abastecimentos/setEnviados");
		
		System.out.println("THIAGO BONATTI - INICIO DA CONEXÃO");
		
		System.out.println(uriBuf);

		URL url;
		try {
			url = new URL(uriBuf.toString());

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setRequestMethod("PUT");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("token", "7107b5a2-34ae-11eb-adc1-0242ac120002");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			
			String jsonInputString = "[" + corpo.replace("INICIO,", "") + "]";
			
			System.out.println("THIAGO BONATTI - CORPO DO JSON");
			
			System.out.println(jsonInputString);
			
			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(jsonInputString.toString());
			wr.flush();
            wr.close();

			//display what returns the POST request

			StringBuilder sb = new StringBuilder();  
			int HttpResult = conn.getResponseCode(); 
			if (HttpResult == HttpURLConnection.HTTP_OK) {
			    BufferedReader br = new BufferedReader(
			            new InputStreamReader(conn.getInputStream(), "utf-8"));
			    String line = null;  
			    while ((line = br.readLine()) != null) {  
			        sb.append(line + "\n");  
			    }
			    br.close();
			    System.out.println("" + sb.toString());  
			} else {
			    System.out.println(conn.getResponseMessage());  
			}  

		   System.out.println("THIAGO BONATTI - FIM DO JSON");
		   
			nativeSql.executeUpdate("UPDATE AD_IMPABAST SET RETORNO = 'S' WHERE NVL(RETORNO,'N') = 'N' ");
		   
	} catch (JSONException e) {
		e.printStackTrace();
	}
		

} catch (Exception e) {
	e.printStackTrace();
	

}
	}
}
