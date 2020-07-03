# 规则链表 rule_chain 
```sql
id uuid 
tenant_id uuid,                 # 租户
name text,                      # 名称
search_text text,               # 可搜索的名称
first_rule_node_id uuid,        # 首节点Id
root boolean,                   # 是否根链
debug_mode boolean,             # 是否调试模式
configuration text,             # 配置文件
additional_info text,           # 增量信息
PRIMARY KEY (id, tenant_id)
```
# 规则节点表rule_node
```sql
id uuid, 
rule_chain_id uuid,             # 规则链Id
type text,                      # 类型（action)
name text,                      # 名称
debug_mode boolean,             # 是否调试模式
search_text text,               # 可供搜索内容
configuration text,             # 配置信息
additional_info text,           # 增量信息
PRIMARY KEY (id)
```
# 节点关系表 relation 
```
from_id timeuuid,              # 来源节点
from_type text,                # 来源节点类型RULE_CHAIN|TENANT|RULE_NODE
to_id timeuuid,                # 目标节点
to_type text,                  # 目标节点类型
relation_type_group text,      # 目标节点类型RULE_CHAIN|RULE_NODE
relation_type text,            # 关系组RULE_CHAIN（Contains），RULE_NODE（Post attributes|Post telemetry）
additional_info text           # 附加信息
```

