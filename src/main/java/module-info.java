import net.yakclient.archive.mapper.ParserProvider;
import net.yakclient.archive.mapper.parsers.DefaultParserProvider;

module yakclient.archive.mapper {
    requires kotlin.stdlib;

    exports net.yakclient.archive.mapper;

    uses ParserProvider;

    provides ParserProvider with DefaultParserProvider;
}