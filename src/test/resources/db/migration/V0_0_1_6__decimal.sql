CREATE TABLE [dbo].[decimal_table] (
  id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
  [value]    DECIMAL(18,2)
);
CREATE TABLE [dbo].[numeric_table] (
  id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
  [value]    NUMERIC(18,2)
);

