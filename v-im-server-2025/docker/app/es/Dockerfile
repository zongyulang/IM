FROM docker.elastic.co/elasticsearch/elasticsearch:9.0.2

# 安装官方内置的 nori（韩文）和 kuromoji（日文）分词器
RUN elasticsearch-plugin install --batch analysis-nori
RUN elasticsearch-plugin install --batch analysis-kuromoji