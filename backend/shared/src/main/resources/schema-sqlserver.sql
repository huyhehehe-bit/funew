-- Schema cho SQL Server 2022.
-- Chay moi lan khoi dong nhung co kiem tra ton tai nen khong ghi de du lieu dang co.
-- Ten bang va ten cot phai khop chinh xac voi anh xa JPA trong cac entity.

IF OBJECT_ID('dbo.accounts', 'U') IS NULL
CREATE TABLE dbo.accounts (
    id            BIGINT         IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name          NVARCHAR(150)  NOT NULL,
    email         NVARCHAR(254)  NOT NULL UNIQUE,
    password_hash NVARCHAR(255)  NOT NULL,
    role          INT            NOT NULL
);

IF OBJECT_ID('dbo.categories', 'U') IS NULL
CREATE TABLE dbo.categories (
    id          BIGINT          IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name        NVARCHAR(150)   NOT NULL,
    description NVARCHAR(2000)  NOT NULL,
    parent_id   BIGINT          NULL,
    is_active   INT             NOT NULL,
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES dbo.categories (id)
);

IF OBJECT_ID('dbo.news_articles', 'U') IS NULL
CREATE TABLE dbo.news_articles (
    id            BIGINT          IDENTITY(1,1) NOT NULL PRIMARY KEY,
    title         NVARCHAR(300)   NOT NULL,
    headline      NVARCHAR(1000)  NOT NULL,
    content       NVARCHAR(MAX)   NOT NULL,
    source        NVARCHAR(500)   NOT NULL,
    category_id   BIGINT          NOT NULL,
    tags          NVARCHAR(1000)  NOT NULL,
    status        INT             NOT NULL,
    created_by_id BIGINT          NOT NULL,
    created_date  DATE            NOT NULL,
    CONSTRAINT fk_news_category FOREIGN KEY (category_id)   REFERENCES dbo.categories (id),
    CONSTRAINT fk_news_author   FOREIGN KEY (created_by_id) REFERENCES dbo.accounts (id)
);
