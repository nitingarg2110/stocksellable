package com.stock.stocksellable.controller;

import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import com.stock.stocksellable.service.MongoDbService;

@RestController
@RequestMapping("/rest")
public class StockSellableController {

	@Autowired
	private MongoDbService mongoDbService;

	private static final Logger LOGGER = LoggerFactory.getLogger(StockSellableController.class.getName());

	@GetMapping("/IsStockSellable/{userName}")
	public JSONObject getUserStocksCurrentPrice(@PathVariable("userName") final String userName)
			throws RestClientException, ParseException, IOException {
		LOGGER.info("Document get {}", userName);

		JSONObject mongoDbResponse = mongoDbService.getPayload(userName);
		JSONObject mongoDbTransientDataResponse = mongoDbService.getTransientData(userName);

		return updateOriginalPayloadWithSellableStatus(mongoDbResponse, mongoDbTransientDataResponse);

	}

	private JSONObject updateOriginalPayloadWithSellableStatus(JSONObject mongoDbResponse,
			JSONObject mongoDbTransientDataResponse) throws RestClientException, ParseException, IOException {

		JSONObject updatedPayload = mongoDbResponse;

		String nodeName = mongoDbTransientDataResponse.get("nodeName").toString();
		String eventName = nodeName.split("_")[1];

		JSONArray mongoDbStockArray = (JSONArray) mongoDbResponse.get("Stocks");

		for (Object stockObject : mongoDbStockArray) {
			String isSellable = String.valueOf(false);
			JSONObject object = (JSONObject) stockObject;
			String stockName = object.get("stockName").toString();
			Double buyPrice = Double.valueOf(object.get("buyPrice").toString());
			Double currentPrice = Double.valueOf(object.get("currentPrice").toString());
			Double thresholdValue = Double.valueOf(object.get("thresholdValue").toString());

			if ("SellAllStocks".equalsIgnoreCase(eventName)) {
				isSellable = String.valueOf(true);
				updatePayLoad(updatedPayload, isSellable, stockName);
			} else {
				if (currentPrice - buyPrice > 0) {
					double profitPercent = ((currentPrice - buyPrice) / buyPrice) * 100;
					if (profitPercent - thresholdValue > 0) {
						isSellable = String.valueOf(true);
						updatePayLoad(updatedPayload, isSellable, stockName);
					}

				}
			}
		}

		JSONObject responeObject = mongoDbService.updatePayload(updatedPayload);
		return responeObject;

	}

	private void updatePayLoad(JSONObject updatedPayload, String isSellable, String stockName) {
		JSONArray mongoDbStockArray = (JSONArray) updatedPayload.get("Stocks");

		for (Object stockObject : mongoDbStockArray) {
			JSONObject object = (JSONObject) stockObject;
			String name = object.get("stockName").toString();
			if (name.equalsIgnoreCase(stockName)) {
				object.put("isSellable", isSellable);
				break;
			}

		}

	}

}
