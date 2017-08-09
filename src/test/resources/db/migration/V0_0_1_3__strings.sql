CREATE TABLE [dbo].[char_table] (
  id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
  [value]    CHAR(128)
);
CREATE TABLE [dbo].[varchar_table] (
  id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
  [value]    VARCHAR(256)
);
CREATE TABLE [dbo].[text_table] (
  id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
  [value]    TEXT
);

CREATE TABLE [dbo].[nchar_table] (
  id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
  [value]    NCHAR(128)
);
CREATE TABLE [dbo].[nvarchar_table] (
  id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
  [value]    NVARCHAR(256)
);
CREATE TABLE [dbo].[ntext_table] (
  id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
  [value]    NTEXT
);
CREATE TABLE [dbo].[uniqueidentifier_table] (
id         BIGINT IDENTITY PRIMARY KEY NOT NULL,
[value]    UNIQUEIDENTIFIER
);
