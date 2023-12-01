#!/bin/bash

echo ""
echo "Applying migration TaxNumber"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /taxNumber                        controllers.TaxNumberController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /taxNumber                        controllers.TaxNumberController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeTaxNumber                  controllers.TaxNumberController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeTaxNumber                  controllers.TaxNumberController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "taxNumber.title = taxNumber" >> ../conf/messages.en
echo "taxNumber.heading = taxNumber" >> ../conf/messages.en
echo "taxNumber.checkYourAnswersLabel = taxNumber" >> ../conf/messages.en
echo "taxNumber.error.required = Enter taxNumber" >> ../conf/messages.en
echo "taxNumber.error.length = TaxNumber must be 100 characters or less" >> ../conf/messages.en
echo "taxNumber.change.hidden = TaxNumber" >> ../conf/messages.en

echo "Migration TaxNumber completed"
